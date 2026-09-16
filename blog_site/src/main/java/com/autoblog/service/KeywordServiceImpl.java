package com.autoblog.service;

import com.autoblog.dao.BlogPostDao;
import com.autoblog.dao.GeneratedDraftDao;
import com.autoblog.dao.KeywordDao;
import com.autoblog.model.BlogPost;
import com.autoblog.model.GeneratedDraft;
import com.autoblog.model.KeywordItem;
import com.autoblog.model.KeywordTrendData;
import com.autoblog.model.KeywordTrendPoint;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class KeywordServiceImpl implements KeywordService {
    private KeywordDao keywordDao;
    private BlogPostDao blogPostDao;
    private GeneratedDraftDao generatedDraftDao;
    private WordpressSiteService wordpressSiteService;
    private String pythonExecutable;
    private String scriptPath;
    private String workingDirectory;
    private final Map<String, KeywordTrendData> trendCache = new HashMap<String, KeywordTrendData>();
    private final Map<String, Long> trendCacheTimes = new HashMap<String, Long>();
    private static final long TREND_CACHE_MILLIS = 60L * 60L * 1000L;

    public void setKeywordDao(KeywordDao keywordDao) { this.keywordDao = keywordDao; }
    public void setBlogPostDao(BlogPostDao blogPostDao) { this.blogPostDao = blogPostDao; }
    public void setGeneratedDraftDao(GeneratedDraftDao generatedDraftDao) { this.generatedDraftDao = generatedDraftDao; }
    public void setWordpressSiteService(WordpressSiteService wordpressSiteService) { this.wordpressSiteService = wordpressSiteService; }
    public void setPythonExecutable(String pythonExecutable) { this.pythonExecutable = pythonExecutable; }
    public void setScriptPath(String scriptPath) { this.scriptPath = scriptPath; }
    public void setWorkingDirectory(String workingDirectory) { this.workingDirectory = workingDirectory; }

    public List<KeywordItem> collectAndSave(String category, int count) {
        try {
            int fetchCount = Math.max(count, Math.min(60, count * 4));
            ProcessBuilder builder = category == null || category.trim().isEmpty()
                    ? new ProcessBuilder(pythonExecutable, scriptPath, "--keywords-json", "", String.valueOf(fetchCount))
                    : new ProcessBuilder(pythonExecutable, scriptPath, "--keywords-json", category, String.valueOf(fetchCount));
            builder.directory(new java.io.File(workingDirectory));
            builder.environment().put("PYTHONIOENCODING", "utf-8");
            builder.environment().put("PYTHONUTF8", "1");
            builder.environment().put("EXCLUDED_TOPICS", buildExcludedTopics(siteIdFromActiveSite()));
            builder.redirectErrorStream(true);
            Process process = builder.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) output.append(line);
            }
            if (process.waitFor() != 0) throw new IllegalStateException(output.toString());
            JsonNode keywords = new ObjectMapper().readTree(output.toString()).path("keywords");
            List<KeywordItem> saved = new ArrayList<KeywordItem>();
            long siteId = wordpressSiteService.getActiveSite().getId();
            if (keywords.isArray()) {
                for (JsonNode node : keywords) {
                    if (saved.size() >= count) break;
                    KeywordItem item = new KeywordItem();
                    item.setSiteId(siteId);
                    item.setKeyword(node.asText());
                    item.setGroupName(category == null || category.trim().isEmpty() ? "자동 수집" : category);
                    item.setStatus("active");
                    if (keywordDao.countExisting(siteId, item.getGroupName(), item.getKeyword()) == 0
                            && !isUsedTopic(siteId, item.getKeyword())) {
                        keywordDao.insert(item);
                        saved.add(item);
                    }
                }
            }
            return saved;
        } catch (Exception e) {
            throw new IllegalStateException("키워드 수집에 실패했습니다.", e);
        }
    }
    public List<KeywordItem> getKeywords(String query) { return keywordDao.findBySite(wordpressSiteService.getActiveSite().getId(), query, null); }
    public List<KeywordItem> getKeywords(String query, String category) {
        return keywordDao.findBySite(wordpressSiteService.getActiveSite().getId(), query, category);
    }
    public int getKeywordCount() { return keywordDao.countBySite(wordpressSiteService.getActiveSite().getId()); }
    public KeywordItem getKeyword(long id) { return keywordDao.findById(wordpressSiteService.getActiveSite().getId(), id); }
    public void deleteKeywords(long[] ids) {
        if (ids == null || ids.length == 0) return;
        keywordDao.deleteByIds(wordpressSiteService.getActiveSite().getId(), ids);
    }
    public void deleteKeywordAfterPublish(long siteId, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return;
        keywordDao.deleteByKeyword(siteId, keyword.trim());
    }

    private boolean isUsedTopic(long siteId, String keyword) {
        String normalizedKeyword = normalizeForSimilarity(keyword);
        if (normalizedKeyword.length() < 4) return false;

        if (blogPostDao != null) {
            List<BlogPost> posts = blogPostDao.findPosts(siteId, null, "all", "all", 80, 0);
            for (BlogPost post : posts) {
                if (post != null && isSimilarKeywordToTitle(normalizedKeyword, post.getTitle())) {
                    return true;
                }
            }
        }

        if (generatedDraftDao != null) {
            List<GeneratedDraft> drafts = generatedDraftDao.findBySite(siteId);
            for (GeneratedDraft draft : drafts) {
                if (draft == null) continue;
                if (isSimilarKeywordToTitle(normalizedKeyword, draft.getTitle())
                        || isSimilarKeywordToTitle(normalizedKeyword, draft.getKeyword())) {
                    return true;
                }
            }
        }
        return false;
    }

    private long siteIdFromActiveSite() {
        return wordpressSiteService.getActiveSite().getId();
    }

    private String buildExcludedTopics(long siteId) {
        StringBuilder builder = new StringBuilder();
        int count = 0;
        if (blogPostDao != null) {
            List<BlogPost> posts = blogPostDao.findPosts(siteId, null, "all", "all", 80, 0);
            for (BlogPost post : posts) {
                if (post == null || post.getTitle() == null || post.getTitle().trim().isEmpty()) continue;
                if (builder.length() > 0) builder.append("\n");
                builder.append("- ").append(post.getTitle().trim());
                if (++count >= 80) break;
            }
        }
        if (generatedDraftDao != null && count < 100) {
            List<GeneratedDraft> drafts = generatedDraftDao.findBySite(siteId);
            for (GeneratedDraft draft : drafts) {
                if (draft == null || draft.getTitle() == null || draft.getTitle().trim().isEmpty()) continue;
                if (builder.length() > 0) builder.append("\n");
                builder.append("- ").append(draft.getTitle().trim());
                if (++count >= 100) break;
            }
        }
        return builder.toString();
    }

    private boolean isSimilarKeywordToTitle(String normalizedKeyword, String titleOrKeyword) {
        String normalizedExisting = normalizeForSimilarity(titleOrKeyword);
        if (normalizedExisting.length() < 4) return false;
        if (normalizedKeyword.equals(normalizedExisting)) return true;
        if (normalizedExisting.contains(normalizedKeyword) || normalizedKeyword.contains(normalizedExisting)) return true;
        return bigramJaccard(normalizedKeyword, normalizedExisting) >= 0.82d;
    }

    private String normalizeForSimilarity(String value) {
        if (value == null) return "";
        return value.toLowerCase()
                .replace("챗gpt", "chatgpt")
                .replace("챗지피티", "chatgpt")
                .replaceAll("\\bgpt\\b", "chatgpt")
                .replace("openai", "오픈ai")
                .replace("overwatch", "오버워치")
                .replaceAll("[\\p{Punct}\\s]+", "")
                .replace("2024년", "")
                .replace("2025년", "")
                .replace("2026년", "")
                .replaceAll("[0-9]+", "")
                .replace("추천", "")
                .replace("정리", "")
                .replace("가이드", "")
                .replace("방법", "")
                .replace("가지", "")
                .trim();
    }

    private boolean sharesTopicToken(Set<String> keywordTokens, String existingTitleOrKeyword) {
        if (keywordTokens == null || keywordTokens.isEmpty()) return false;
        Set<String> existingTokens = topicTokens(existingTitleOrKeyword);
        for (String token : keywordTokens) {
            if (existingTokens.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> topicTokens(String value) {
        Set<String> result = new HashSet<String>();
        if (value == null) return result;
        String text = value.toLowerCase()
                .replace("챗gpt", "chatgpt")
                .replace("챗지피티", "chatgpt")
                .replaceAll("\\bgpt\\b", "chatgpt")
                .replace("openai", "오픈ai")
                .replace("overwatch", "오버워치")
                .replaceAll("[0-9]+", " ")
                .replaceAll("[\\p{Punct}]+", " ");
        String[] tokens = text.split("\\s+");
        for (String token : tokens) {
            token = normalizeTopicToken(token);
            if (isMeaningfulTopicToken(token)) {
                result.add(token);
            }
        }
        return result;
    }

    private String normalizeTopicToken(String token) {
        if (token == null) return "";
        token = token.trim();
        token = token.replaceAll("(으로|로|에게|에서|부터|까지|처럼|보다|은|는|이|가|을|를|의|와|과|도|만)$", "");
        token = token.replace("하는", "").replace("하기", "").replace("작성하는", "작성");
        return token.trim();
    }

    private boolean isMeaningfulTopicToken(String token) {
        if (token == null || token.length() < 2) return false;
        if ("ai".equals(token) || "it".equals(token) || "글".equals(token) || "블로그".equals(token)) return false;
        if ("자동".equals(token) || "자동화".equals(token) || "작성".equals(token) || "생성".equals(token)) return false;
        if ("추천".equals(token) || "정리".equals(token) || "가이드".equals(token) || "방법".equals(token)) return false;
        if ("최신".equals(token) || "초보자".equals(token) || "실용".equals(token) || "총정리".equals(token)) return false;
        if ("사용법".equals(token) || "예시".equals(token) || "리스트".equals(token) || "꿀팁".equals(token)) return false;
        if ("생활".equals(token) || "게임".equals(token)) return false;
        return true;
    }

    private double bigramJaccard(String a, String b) {
        Set<String> left = bigrams(a);
        Set<String> right = bigrams(b);
        if (left.isEmpty() || right.isEmpty()) return 0d;
        Set<String> intersection = new HashSet<String>(left);
        intersection.retainAll(right);
        Set<String> union = new HashSet<String>(left);
        union.addAll(right);
        return union.isEmpty() ? 0d : (double) intersection.size() / (double) union.size();
    }

    private Set<String> bigrams(String value) {
        Set<String> result = new HashSet<String>();
        if (value == null) return result;
        if (value.length() <= 2) {
            result.add(value);
            return result;
        }
        for (int i = 0; i < value.length() - 1; i++) {
            result.add(value.substring(i, i + 2));
        }
        return result;
    }

    public KeywordTrendData getTrend(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return null;
        try {
            String cacheKey = keyword.trim().toLowerCase();
            Long cachedAt = trendCacheTimes.get(cacheKey);
            if (cachedAt != null && System.currentTimeMillis() - cachedAt.longValue() < TREND_CACHE_MILLIS) {
                return trendCache.get(cacheKey);
            }
            ProcessBuilder builder = new ProcessBuilder(pythonExecutable, scriptPath, "--trend-json", keyword);
            builder.directory(new java.io.File(workingDirectory));
            builder.environment().put("PYTHONIOENCODING", "utf-8");
            builder.environment().put("PYTHONUTF8", "1");
            builder.redirectErrorStream(true);
            Process process = builder.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) output.append(line);
            }
            if (process.waitFor() != 0) throw new IllegalStateException(output.toString());
            JsonNode root = new ObjectMapper().readTree(output.toString());
            KeywordTrendData data = new KeywordTrendData();
            data.setKeyword(root.path("keyword").asText());
            data.setStartDate(root.path("startDate").asText());
            data.setEndDate(root.path("endDate").asText());
            List<KeywordTrendPoint> points = new ArrayList<KeywordTrendPoint>();
            for (JsonNode node : root.path("points")) {
                KeywordTrendPoint point = new KeywordTrendPoint();
                point.setPeriod(node.path("period").asText());
                point.setRatio(node.path("ratio").asDouble());
                points.add(point);
            }
            data.setPoints(points);
            trendCache.put(cacheKey, data);
            trendCacheTimes.put(cacheKey, System.currentTimeMillis());
            return data;
        } catch (Exception e) {
            throw new IllegalStateException("키워드 추세 조회에 실패했습니다.", e);
        }
    }
}
