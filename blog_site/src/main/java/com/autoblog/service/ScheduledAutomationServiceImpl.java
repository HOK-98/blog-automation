package com.autoblog.service;

import com.autoblog.model.GeneratedDraft;
import com.autoblog.model.GenerationPreview;
import com.autoblog.model.KeywordItem;
import com.autoblog.model.WordpressSite;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledAutomationServiceImpl implements ScheduledAutomationService {
    private WordpressSiteService wordpressSiteService;
    private WordpressPostService wordpressPostService;
    private KeywordService keywordService;
    private GenerationService generationService;
    private GeneratedDraftService generatedDraftService;
    private WordpressSyncService wordpressSyncService;
    private ScheduledExecutorService executor;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private long lastViewSyncAt;
    private volatile boolean automationRunning;
    private static final long VIEW_SYNC_INTERVAL_MILLIS = 30L * 60L * 1000L;

    public void setWordpressSiteService(WordpressSiteService wordpressSiteService) { this.wordpressSiteService = wordpressSiteService; }
    public void setWordpressPostService(WordpressPostService wordpressPostService) { this.wordpressPostService = wordpressPostService; }
    public void setKeywordService(KeywordService keywordService) { this.keywordService = keywordService; }
    public void setGenerationService(GenerationService generationService) { this.generationService = generationService; }
    public void setGeneratedDraftService(GeneratedDraftService generatedDraftService) { this.generatedDraftService = generatedDraftService; }
    public void setWordpressSyncService(WordpressSyncService wordpressSyncService) { this.wordpressSyncService = wordpressSyncService; }

    public void start() {
        if (executor != null) return;
        // Spring 컨텍스트가 뜰 때 한 번 시작되는 간단한 내장 스케줄러입니다.
        // 서버가 켜져 있는 동안 1분마다 설정값을 확인하고, 조건이 맞으면 자동화를 실행합니다.
        // 외부 배치 서버나 Quartz 없이 동작시키기 위한 최소 구조입니다.
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                try {
                    tick();
                } catch (Exception e) {
                    System.err.println("[AutoBlog scheduler] " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }, 10, 60, TimeUnit.SECONDS);
    }

    public void stop() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    public void runNow() {
        WordpressSite site = wordpressSiteService.getActiveSite();
        if (site == null) {
            throw new IllegalStateException("활성 사이트가 없습니다.");
        }
        // 수동 실행은 예약 실행일을 갱신하지 않습니다.
        // 사용자가 버튼으로 테스트하거나 추가 실행해도 오늘 예약 실행 자체는 유지됩니다.
        runExclusive(site, false);
    }

    public boolean runNowAsync() {
        final WordpressSite site = wordpressSiteService.getActiveSite();
        if (site == null) {
            throw new IllegalStateException("활성 사이트가 없습니다.");
        }
        start();
        synchronized (this) {
            if (automationRunning) {
                return false;
            }
            automationRunning = true;
        }
        executor.submit(new Runnable() {
            public void run() {
                try {
                    System.out.println("[AutoBlog scheduler] manual automation started site=" + site.getName());
                    runForActiveSite(site);
                    System.out.println("[AutoBlog scheduler] manual automation finished site=" + site.getName());
                } catch (Exception e) {
                    System.err.println("[AutoBlog scheduler] manual automation failed: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    automationRunning = false;
                }
            }
        });
        return true;
    }

    private void tick() {
        WordpressSite site = wordpressSiteService.getActiveSite();
        syncViewsIfNeeded(site);
        if (site == null || !site.isScheduleEnabled()) return;
        if (!isDue(site)) return;

        // 현재 버전은 "활성 사이트" 기준으로만 실행합니다.
        // 여러 사이트를 각각 다른 시간에 돌리려면 사이트별 실행 컨텍스트를 분리하면 됩니다.
        runExclusive(site, true);
    }

    private void runExclusive(WordpressSite site, boolean markScheduledRun) {
        synchronized (this) {
            if (automationRunning) {
                System.out.println("[AutoBlog scheduler] automation already running. skipped site=" + site.getName());
                return;
            }
            automationRunning = true;
        }
        try {
            runForActiveSite(site);
            if (markScheduledRun) {
                wordpressSiteService.markScheduledRun(site.getId());
            }
        } finally {
            automationRunning = false;
        }
    }

    private void syncViewsIfNeeded(WordpressSite site) {
        if (site == null || wordpressSyncService == null) return;
        long now = System.currentTimeMillis();
        if (now - lastViewSyncAt < VIEW_SYNC_INTERVAL_MILLIS) return;
        try {
            // 자동 발행 직후에는 로컬 DB에 조회수 0으로 들어갑니다.
            // 시간이 지나 GA4에 데이터가 쌓이면 주기적으로 워드프레스/GA4 동기화를 돌려 조회수를 갱신합니다.
            wordpressSyncService.syncPosts();
            lastViewSyncAt = now;
            System.out.println("[AutoBlog scheduler] view metrics synced site=" + site.getName());
        } catch (Exception e) {
            lastViewSyncAt = now;
            System.err.println("[AutoBlog scheduler] view sync failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isDue(WordpressSite site) {
        String rawTime = site.getScheduleStartTime();
        if (rawTime == null || rawTime.trim().isEmpty()) return false;
        Date lastRun = site.getLastScheduledRunDate();
        LocalDate today = LocalDate.now();
        // 같은 날짜에 중복 실행되지 않도록 DB에 마지막 실행일을 저장합니다.
        if (lastRun != null && today.equals(lastRun.toLocalDate())) return false;

        LocalTime target = LocalTime.parse(rawTime.trim(), timeFormatter);
        LocalTime now = LocalTime.now();
        return !now.isBefore(target);
    }

    private void runForActiveSite(WordpressSite site) {
        if (!site.isAutoContentGeneration()) return;
        int postCount = safe(site.getScheduledPostCount(), 1, 1, 10);
        boolean fixedCategory = site.getScheduleCategory() != null && !site.getScheduleCategory().trim().isEmpty();
        int generated = 0;
        int published = 0;
        int heldForReview = 0;
        int failed = 0;
        String selectedCategory = fixedCategory ? site.getScheduleCategory().trim() : "all";
        List<KeywordItem> keywords = keywordService.getKeywords(null, selectedCategory);
        if (!fixedCategory) {
            keywords = spreadKeywordsByCategory(keywords, postCount);
        }
        if (keywords.isEmpty()) {
            System.out.println("[AutoBlog scheduler] no queued keywords. site=" + site.getName() + " category=" + selectedCategory);
            return;
        }

        for (KeywordItem item : keywords) {
            if (generated >= postCount) break;
            try {
                String category = fixedCategory ? site.getScheduleCategory().trim() : item.getGroupName();
                if ("자동 수집".equals(category) || "all".equalsIgnoreCase(category)) {
                    category = null;
                }
                Long categoryId = resolveCategoryId(site, category);
                GenerationPreview preview = generationService.generatePreview(item.getKeyword());
                GeneratedDraft draft = generatedDraftService.savePreview(preview);
                // 예약/즉시 실행은 키워드 목록에 남겨둔 항목을 신뢰해서 생성합니다.
                // 카테고리도 키워드의 그룹명 또는 설정에서 선택한 카테고리를 그대로 사용합니다.
                if (category != null && !category.trim().isEmpty()) {
                    draft.setCategories(category);
                }
                draft.setWordpressCategoryId(categoryId);
                generatedDraftService.updateDraft(draft);
                if (site.isAutoPublishing() && !preview.isCriticalFactualError()) {
                    if (!preview.isApproved()) {
                        System.out.println("[AutoBlog scheduler] publishing despite review flag. keyword=" + item.getKeyword()
                                + " approved=" + preview.isApproved()
                                + " score=" + preview.getScore());
                    }
                    generatedDraftService.publishDraft(draft.getId());
                    published++;
                } else {
                    heldForReview++;
                    System.out.println("[AutoBlog scheduler] draft held for review. keyword=" + item.getKeyword()
                            + " approved=" + preview.isApproved()
                            + " criticalFactualError=" + preview.isCriticalFactualError()
                            + " autoPublishing=" + site.isAutoPublishing()
                            + " score=" + preview.getScore());
                }
                generated++;
            } catch (IllegalStateException e) {
                failed++;
                System.err.println("[AutoBlog scheduler] skipped: " + e.getMessage());
            } catch (Exception e) {
                failed++;
                System.err.println("[AutoBlog scheduler] generation failed: " + e.getMessage());
            }
        }
        System.out.println("[AutoBlog scheduler] finished site=" + site.getName()
                + " generated=" + generated
                + " published=" + published
                + " review=" + heldForReview
                + " failed=" + failed);
    }

    private List<KeywordItem> spreadKeywordsByCategory(List<KeywordItem> keywords, int limit) {
        List<KeywordItem> result = new ArrayList<KeywordItem>();
        if (keywords == null || keywords.isEmpty()) {
            return result;
        }

        Map<String, List<KeywordItem>> grouped = new LinkedHashMap<String, List<KeywordItem>>();
        for (KeywordItem item : keywords) {
            String groupName = normalizeGroupName(item == null ? null : item.getGroupName());
            if (!grouped.containsKey(groupName)) {
                grouped.put(groupName, new ArrayList<KeywordItem>());
            }
            grouped.get(groupName).add(item);
        }

        List<String> groupNames = new ArrayList<String>(grouped.keySet());
        Collections.shuffle(groupNames);

        int index = 0;
        while (result.size() < limit && !groupNames.isEmpty()) {
            boolean added = false;
            for (String groupName : groupNames) {
                List<KeywordItem> groupItems = grouped.get(groupName);
                if (groupItems != null && index < groupItems.size()) {
                    result.add(groupItems.get(index));
                    added = true;
                    if (result.size() >= limit) {
                        break;
                    }
                }
            }
            if (!added) {
                break;
            }
            index++;
        }

        System.out.println("[AutoBlog scheduler] auto category spread selected="
                + summarizeSelectedGroups(result)
                + " totalCandidates=" + keywords.size());
        return result;
    }

    private String normalizeGroupName(String groupName) {
        if (groupName == null || groupName.trim().isEmpty()) {
            return "자동 수집";
        }
        return groupName.trim();
    }

    private String summarizeSelectedGroups(List<KeywordItem> items) {
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        for (KeywordItem item : items) {
            String groupName = normalizeGroupName(item == null ? null : item.getGroupName());
            Integer count = counts.get(groupName);
            counts.put(groupName, count == null ? 1 : count + 1);
        }
        StringBuilder summary = new StringBuilder();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (summary.length() > 0) summary.append(", ");
            summary.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return summary.toString();
    }

    private String resolveRandomCategory(WordpressSite site) {
        if (site.getScheduleCategory() != null && !site.getScheduleCategory().trim().isEmpty()) {
            return site.getScheduleCategory().trim();
        }
        if (wordpressPostService == null) {
            return null;
        }
        try {
            java.util.List<com.autoblog.model.WordpressCategory> categories = wordpressPostService.getCategories(site.getId());
            java.util.List<com.autoblog.model.WordpressCategory> candidates = new ArrayList<com.autoblog.model.WordpressCategory>();
            for (com.autoblog.model.WordpressCategory category : categories) {
                String name = category.getName();
                if (name != null && !name.trim().isEmpty() && !"Uncategorized".equalsIgnoreCase(name.trim()) && !"미분류".equals(name.trim())) {
                    candidates.add(category);
                }
            }
            if (!candidates.isEmpty()) {
                // 자동 선택이 항상 첫 번째 카테고리만 고르면 AI 같은 특정 주제가 반복되므로 랜덤 선택합니다.
                Collections.shuffle(candidates);
                return candidates.get(0).getName().trim();
            }
        } catch (Exception e) {
            System.err.println("[AutoBlog scheduler] category fallback failed: " + e.getMessage());
        }
        return null;
    }

    private Long resolveCategoryId(WordpressSite site, String selectedCategoryName) {
        if (wordpressPostService == null || selectedCategoryName == null || selectedCategoryName.trim().isEmpty()) {
            return null;
        }
        try {
            java.util.List<com.autoblog.model.WordpressCategory> categories = wordpressPostService.getCategories(site.getId());
            String wanted = normalizeCategory(selectedCategoryName);
            for (com.autoblog.model.WordpressCategory category : categories) {
                String name = normalizeCategory(category.getName());
                String slug = normalizeCategory(category.getSlug());
                if (wanted.equals(name) || wanted.equals(slug)) {
                    return category.getId();
                }
            }
            System.err.println("[AutoBlog scheduler] category id not found. selected=" + selectedCategoryName);
        } catch (Exception e) {
            System.err.println("[AutoBlog scheduler] category id lookup failed: " + e.getMessage());
        }
        return null;
    }

    private String normalizeCategory(String value) {
        if (value == null) return "";
        return value.trim()
                .toLowerCase()
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "")
                .replace("/", "");
    }

    private int safe(int value, int fallback, int min, int max) {
        if (value <= 0) value = fallback;
        return Math.max(min, Math.min(max, value));
    }
}
