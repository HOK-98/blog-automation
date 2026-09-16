package com.autoblog.service;

import com.autoblog.dao.BlogPostDao;
import com.autoblog.model.BlogPost;
import com.autoblog.dao.GeneratedDraftDao;
import com.autoblog.model.GeneratedDraft;
import com.autoblog.model.GenerationPreview;
import com.autoblog.model.WordpressSite;
import com.autoblog.model.WordpressCategory;
import com.autoblog.model.GeneratedImage;
import com.autoblog.model.OfficialImageSource;
import com.autoblog.model.WordpressMediaResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.autoblog.model.WordpressPostResult;
import com.autoblog.util.EncodingUtil;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeneratedDraftServiceImpl implements GeneratedDraftService {
    private GeneratedDraftDao generatedDraftDao;
    private WordpressSiteService wordpressSiteService;
    private WordpressPostService wordpressPostService;
    private WordpressSyncService wordpressSyncService;
    private KeywordService keywordService;
    private BlogPostDao blogPostDao;
    private ImageGenerationService imageGenerationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void setGeneratedDraftDao(GeneratedDraftDao generatedDraftDao) { this.generatedDraftDao = generatedDraftDao; }
    public void setWordpressSiteService(WordpressSiteService wordpressSiteService) { this.wordpressSiteService = wordpressSiteService; }
    public void setWordpressPostService(WordpressPostService wordpressPostService) { this.wordpressPostService = wordpressPostService; }
    public void setWordpressSyncService(WordpressSyncService wordpressSyncService) { this.wordpressSyncService = wordpressSyncService; }
    public void setKeywordService(KeywordService keywordService) { this.keywordService = keywordService; }
    public void setBlogPostDao(BlogPostDao blogPostDao) { this.blogPostDao = blogPostDao; }
    public void setImageGenerationService(ImageGenerationService imageGenerationService) { this.imageGenerationService = imageGenerationService; }

    public GeneratedDraft savePreview(GenerationPreview preview) {
        // Python 자동화 엔진이 만든 미리보기 결과를 바로 워드프레스에 올리지 않고,
        // 사람이 검토/수정할 수 있도록 generated_drafts 테이블에 저장합니다.
        GeneratedDraft draft = new GeneratedDraft();
        WordpressSite site = wordpressSiteService.getActiveSite();
        draft.setSiteId(site.getId());
        draft.setKeyword(EncodingUtil.repairMojibake(preview.getKeyword()));
        draft.setTitle(EncodingUtil.repairMojibake(preview.getTitle()));
        draft.setSlug(EncodingUtil.repairMojibake(preview.getSlug()));
        draft.setExcerpt(EncodingUtil.repairMojibake(preview.getExcerpt()));
        draft.setContentHtml(EncodingUtil.repairMojibake(preview.getContentHtml()));
        draft.setTags(join(preview.getTags()));
        draft.setCategories(join(preview.getCategories()));
        draft.setVerificationScore(preview.getScore());
        draft.setApproved(preview.isApproved());
        draft.setIssues(join(preview.getIssues()));
        draft.setPromptTokens(preview.getPromptTokens());
        draft.setCompletionTokens(preview.getCompletionTokens());
        draft.setTotalTokens(preview.getTotalTokens());
        draft.setOfficialImages(preview.getOfficialImages());
        draft.setStatus("review");
        generatedDraftDao.insert(draft);
        if (keywordService != null) {
            // 글 생성까지 성공한 키워드는 검토 목록 또는 발행 글에서 이어서 관리하므로
            // 키워드 후보 목록에서는 제거합니다.
            keywordService.deleteKeywordAfterPublish(site.getId(), draft.getKeyword());
        }
        return draft;
    }

    public java.util.List<GeneratedDraft> getDrafts() {
        return generatedDraftDao.findBySite(wordpressSiteService.getActiveSite().getId());
    }

    public java.util.List<GeneratedDraft> getDrafts(Long siteId, boolean allSites) {
        if (allSites) return generatedDraftDao.findAll();
        long targetSiteId = siteId == null ? wordpressSiteService.getActiveSite().getId() : siteId.longValue();
        return generatedDraftDao.findBySite(targetSiteId);
    }

    public GeneratedDraft getDraft(long id) {
        return generatedDraftDao.findById(wordpressSiteService.getActiveSite().getId(), id);
    }

    public void updateDraft(GeneratedDraft draft) {
        draft.setSiteId(wordpressSiteService.getActiveSite().getId());
        repairDraft(draft);
        generatedDraftDao.update(draft);
    }

    public void deleteDraft(long id) {
        generatedDraftDao.delete(wordpressSiteService.getActiveSite().getId(), id);
    }

    public void deleteDrafts(long[] ids) {
        if (ids == null || ids.length == 0) return;
        generatedDraftDao.deleteByIds(wordpressSiteService.getActiveSite().getId(), ids);
    }

    public void publishDraft(long id) {
        GeneratedDraft draft = getDraft(id);
        if (draft == null) return;
        repairDraft(draft);
        WordpressSite site = wordpressSiteService.getActiveSite();
        // 설정값을 발행 직전에 반영합니다.
        // - SEO 제목 접미사
        // - 메타 설명 템플릿
        // - 내부 링크 자동 추가
        String title = applyTitleSuffix(draft.getTitle(), site.getSeoTitleSuffix());
        String excerpt = applyMetaTemplate(draft.getExcerpt(), draft, site.getMetaDescriptionTemplate());
        String content = site.isAutoInternalLinks() ? appendInternalLinks(site.getId(), draft.getContentHtml()) : draft.getContentHtml();
        Long categoryId = resolveCategoryIdBeforePublish(site, draft);
        List<WordpressMediaResult> inlineImages = collectAndUploadInlineImages(site, draft, 2);
        if (!inlineImages.isEmpty()) {
            content = insertInlineImages(content, inlineImages);
        }
        WordpressMediaResult featuredMedia = inlineImages.isEmpty() ? null : inlineImages.get(0);
        WordpressPostResult result = wordpressPostService.createPost(
                site.getId(),
                title,
                draft.getSlug(),
                excerpt,
                content,
                "publish",
                categoryId,
                featuredMedia == null ? null : featuredMedia.getId(),
                featuredMedia == null ? null : featuredMedia.getSourceUrl());
        draft.setTitle(title);
        draft.setExcerpt(excerpt);
        draft.setContentHtml(content);
        draft.setWordpressCategoryId(categoryId);
        // 워드프레스 전체 동기화를 기다리지 않고 대시보드 카운트가 즉시 바뀌도록,
        // WordPress 생성 응답의 post id/link를 사용해 blog_posts에도 바로 반영합니다.
        savePublishedPostToLocalDb(site, draft, result);
        if (site.isNotificationEnabled() && site.isNotifyOnPublish()) {
            System.out.println("[AutoBlog notification] Published: " + title + " -> " + result.getLink() + " / to=" + site.getNotificationEmail());
        }
        if (keywordService != null) {
            keywordService.deleteKeywordAfterPublish(site.getId(), draft.getKeyword());
        }
        // 발행이 끝난 글은 blog_posts에서 관리하므로 검토 목록에서는 제거합니다.
        generatedDraftDao.delete(site.getId(), draft.getId());
    }

    private String join(java.util.List<String> values) {
        if (values == null || values.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (sb.length() > 0) sb.append(",");
            sb.append(value);
        }
        return sb.toString();
    }

    private void repairDraft(GeneratedDraft draft) {
        // 과거에 깨져 저장된 mojibake 문자열을 발행/저장 직전에 한 번 복구합니다.
        // 이미 � 대체문자로 손실된 데이터는 복구할 수 없지만, ë/ì/í 형태는 대부분 복원됩니다.
        draft.setKeyword(EncodingUtil.repairMojibake(draft.getKeyword()));
        draft.setTitle(EncodingUtil.repairMojibake(draft.getTitle()));
        draft.setSlug(EncodingUtil.repairMojibake(draft.getSlug()));
        draft.setExcerpt(EncodingUtil.repairMojibake(draft.getExcerpt()));
        draft.setContentHtml(EncodingUtil.repairMojibake(draft.getContentHtml()));
        draft.setTags(EncodingUtil.repairMojibake(draft.getTags()));
        draft.setCategories(EncodingUtil.repairMojibake(draft.getCategories()));
        draft.setIssues(EncodingUtil.repairMojibake(draft.getIssues()));
        draft.setOfficialImages(EncodingUtil.repairMojibake(draft.getOfficialImages()));
    }

    private void savePublishedPostToLocalDb(WordpressSite site, GeneratedDraft draft, WordpressPostResult result) {
        if (blogPostDao == null || result == null || result.getId() <= 0) {
            return;
        }
        BlogPost post = new BlogPost();
        post.setSiteId(site.getId());
        post.setWordpressPostId(result.getId());
        post.setTitle(draft.getTitle());
        post.setSummary(stripHtml(draft.getExcerpt()));
        post.setContent(draft.getContentHtml());
        post.setCategory(emptyToDefault(draft.getCategories(), "미분류"));
        post.setStatus("발행됨");
        post.setTags(emptyToDefault(draft.getTags(), ""));
        post.setCoverImageUrl(result == null ? "" : emptyToDefault(result.getFeaturedMediaUrl(), ""));
        post.setWordpressUrl(result.getLink());
        post.setViewCount(0);
        LocalDateTime now = LocalDateTime.now();
        post.setPublishedAt(now);
        post.setUpdatedAt(now);
        blogPostDao.upsertWordpressPost(post);
    }

    private String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String emptyToDefault(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }


    private List<WordpressMediaResult> collectAndUploadInlineImages(WordpressSite site, GeneratedDraft draft, int maxCount) {
        List<WordpressMediaResult> officialImages = uploadOfficialSourceImages(site, draft, maxCount);
        if (!officialImages.isEmpty()) {
            return officialImages;
        }
        return generateAndUploadInlineImages(site, draft, maxCount);
    }

    private List<WordpressMediaResult> uploadOfficialSourceImages(WordpressSite site, GeneratedDraft draft, int maxCount) {
        List<WordpressMediaResult> results = new ArrayList<WordpressMediaResult>();
        if (site == null || !site.isAutoImageGeneration() || wordpressPostService == null || !shouldAttachInlineImages(draft)) {
            return results;
        }
        List<OfficialImageSource> sources = parseOfficialImages(draft.getOfficialImages());
        int index = 1;
        for (OfficialImageSource source : sources) {
            if (results.size() >= maxCount) break;
            try {
                if (!isAllowedOfficialImage(source)) continue;
                DownloadedImage downloaded = downloadOfficialImage(source.getImageUrl(), draft, index);
                WordpressMediaResult media = wordpressPostService.uploadMedia(site.getId(), downloaded.bytes, downloaded.filename, emptyToDefault(source.getAlt(), draft.getTitle()));
                media.setAltText(emptyToDefault(source.getAlt(), draft.getTitle()));
                media.setCaption(emptyToDefault(source.getCaption(), "공식 출처 참고 이미지"));
                media.setExternalSourceUrl(source.getSourceUrl());
                media.setExternalSourceTitle(emptyToDefault(source.getSourceTitle(), "공식 출처"));
                results.add(media);
                System.out.println("[AutoBlog image] official media uploaded. draftId=" + draft.getId() + " mediaId=" + media.getId() + " source=" + source.getSourceUrl());
            } catch (Exception e) {
                System.err.println("[AutoBlog image] official image skipped: " + e.getMessage());
            }
            index++;
        }
        return results;
    }

    private List<OfficialImageSource> parseOfficialImages(String rawJson) {
        List<OfficialImageSource> result = new ArrayList<OfficialImageSource>();
        if (rawJson == null || rawJson.trim().isEmpty()) return result;
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            if (!root.isArray()) return result;
            for (JsonNode item : root) {
                OfficialImageSource source = new OfficialImageSource();
                source.setImageUrl(item.path("image_url").asText(""));
                source.setSourceUrl(item.path("source_url").asText(""));
                source.setSourceTitle(item.path("source_title").asText(""));
                source.setCaption(item.path("caption").asText(""));
                source.setAlt(item.path("alt").asText(""));
                source.setLicense(item.path("license").asText(""));
                result.add(source);
            }
        } catch (Exception e) {
            System.err.println("[AutoBlog image] official image JSON parse failed: " + e.getMessage());
        }
        return result;
    }

    private boolean isAllowedOfficialImage(OfficialImageSource source) {
        if (source == null) return false;
        String imageUrl = source.getImageUrl() == null ? "" : source.getImageUrl().trim().toLowerCase();
        String sourceUrl = source.getSourceUrl() == null ? "" : source.getSourceUrl().trim().toLowerCase();
        if (!(imageUrl.startsWith("https://") || imageUrl.startsWith("http://"))) return false;
        if (!(sourceUrl.startsWith("https://") || sourceUrl.startsWith("http://"))) return false;
        if (imageUrl.contains("google.com/search") || imageUrl.contains("bing.com") || imageUrl.contains("naver.com/search")) return false;
        if (sourceUrl.contains("google.com/search") || sourceUrl.contains("bing.com") || sourceUrl.contains("naver.com/search")) return false;
        return true;
    }

    private DownloadedImage downloadOfficialImage(String imageUrl, GeneratedDraft draft, int index) throws Exception {
        URL url = new URL(imageUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(7000);
        connection.setReadTimeout(20000);
        connection.setRequestProperty("User-Agent", "AutoBlog/1.0 official image fetcher");
        int code = connection.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new IllegalStateException("HTTP " + code);
        }
        String contentType = connection.getContentType() == null ? "" : connection.getContentType().toLowerCase();
        if (!contentType.startsWith("image/")) {
            throw new IllegalStateException("not an image content-type: " + contentType);
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        try (InputStream input = connection.getInputStream()) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > 5 * 1024 * 1024) {
                    throw new IllegalStateException("image is larger than 5MB");
                }
                output.write(buffer, 0, read);
            }
        }
        String extension = extensionForContentType(contentType);
        return new DownloadedImage(output.toByteArray(), buildOfficialImageFilename(draft, index, extension));
    }

    private String extensionForContentType(String contentType) {
        if (contentType == null) return "jpg";
        if (contentType.contains("png")) return "png";
        if (contentType.contains("webp")) return "webp";
        if (contentType.contains("gif")) return "gif";
        if (contentType.contains("jpeg") || contentType.contains("jpg")) return "jpg";
        return "jpg";
    }

    private String buildOfficialImageFilename(GeneratedDraft draft, int index, String extension) {
        String base = draft == null || draft.getSlug() == null || draft.getSlug().trim().isEmpty() ? "official-source-image" : draft.getSlug().trim();
        base = base.toLowerCase().replaceAll("[^a-z0-9가-힣-_]+", "-").replaceAll("^-|-$", "");
        if (base.length() == 0) base = "official-source-image";
        if (base.length() > 70) base = base.substring(0, 70).replaceAll("-$", "");
        return base + "-official-" + index + "." + extension;
    }

    private static class DownloadedImage {
        private final byte[] bytes;
        private final String filename;
        private DownloadedImage(byte[] bytes, String filename) {
            this.bytes = bytes;
            this.filename = filename;
        }
    }

    private List<WordpressMediaResult> generateAndUploadInlineImages(WordpressSite site, GeneratedDraft draft, int maxCount) {
        List<WordpressMediaResult> results = new ArrayList<WordpressMediaResult>();
        if (site == null || !site.isAutoImageGeneration() || imageGenerationService == null || wordpressPostService == null) {
            return results;
        }
        if (!shouldAttachInlineImages(draft)) {
            System.out.println("[AutoBlog image] skipped: article does not need inline images. draftId=" + draft.getId());
            return results;
        }
        List<String> sectionTitles = pickImageSectionTitles(draft.getContentHtml(), maxCount);
        if (sectionTitles.isEmpty()) {
            System.out.println("[AutoBlog image] skipped: no suitable section for inline image. draftId=" + draft.getId());
            return results;
        }
        int index = 1;
        for (String sectionTitle : sectionTitles) {
            if (results.size() >= maxCount) break;
            try {
                GeneratedImage image = imageGenerationService.generateInlineImage(site, draft, sectionTitle, index);
                WordpressMediaResult media = wordpressPostService.uploadMedia(site.getId(), image.getBytes(), image.getFilename(), image.getAltText());
                media.setAltText(sectionTitle);
                results.add(media);
                System.out.println("[AutoBlog image] inline media uploaded. draftId=" + draft.getId() + " mediaId=" + media.getId() + " section=" + sectionTitle);
            } catch (Exception e) {
                // 이미지 생성/업로드 실패가 글 발행 전체를 막으면 자동화 안정성이 떨어지므로 경고만 남기고 발행은 계속합니다.
                System.err.println("[AutoBlog image] skipped section image: " + e.getMessage());
            }
            index++;
        }
        return results;
    }

    private boolean shouldAttachInlineImages(GeneratedDraft draft) {
        if (draft == null || draft.getContentHtml() == null || draft.getContentHtml().trim().isEmpty()) {
            return false;
        }
        String content = draft.getContentHtml();
        if (Pattern.compile("<img\\s", Pattern.CASE_INSENSITIVE).matcher(content).find()) {
            return false;
        }
        int textLength = stripHtml(content).replaceAll("\\s+", "").length();
        if (textLength < 1200) {
            return false;
        }
        List<String> sections = pickImageSectionTitles(content, 2);
        if (sections.isEmpty()) {
            return false;
        }
        String categoryAndTags = ((draft.getCategories() == null ? "" : draft.getCategories()) + " " + (draft.getTags() == null ? "" : draft.getTags())).toLowerCase();
        String title = draft.getTitle() == null ? "" : draft.getTitle().toLowerCase();
        String topicText = categoryAndTags + " " + title;

        // 게임/기기/생활 팁/비교 글처럼 장면이나 절차를 시각화하면 이해가 빨라지는 글은 이미지가 잘 맞습니다.
        if (containsAny(topicText, new String[] {
                "게임", "오버워치", "롤", "스팀", "pc", "기기", "제품", "전기", "자전거",
                "생활", "인테리어", "요리", "운동", "비교", "방법", "설정", "위치", "포지셔닝", "사례"
        })) {
            return true;
        }

        // 주제가 명확하지 않아도 h2가 사례/단계/비교 중심이면 이미지가 도움이 됩니다.
        for (String section : sections) {
            String normalized = section.toLowerCase();
            if (containsAny(normalized, new String[] {"사례", "단계", "비교", "구조", "흐름", "위치", "방법", "설정", "상황", "예시"})) {
                return true;
            }
        }
        return false;
    }

    private List<String> pickImageSectionTitles(String content, int maxCount) {
        List<String> titles = new ArrayList<String>();
        if (content == null || content.trim().isEmpty()) return titles;

        Pattern pattern = Pattern.compile("<h2[^>]*>(.*?)</h2>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        while (matcher.find() && titles.size() < maxCount) {
            String title = stripHtml(matcher.group(1));
            if (isGoodImageSectionTitle(title)) {
                titles.add(title);
            }
        }
        return titles;
    }

    private boolean isGoodImageSectionTitle(String title) {
        if (title == null) return false;
        String value = title.trim();
        if (value.length() < 2) return false;
        if (value.length() > 55) return false;
        String normalized = value.replace(" ", "");
        if (normalized.contains("FAQ") || normalized.contains("자주묻는질문")) return false;
        if (normalized.contains("마무리") || normalized.contains("결론")) return false;
        if (normalized.contains("함께읽으면") || normalized.contains("관련글")) return false;
        if (normalized.contains("핵심요약") || normalized.contains("요약")) return false;
        if (normalized.contains("개요") || normalized.contains("소개")) return false;
        return true;
    }

    private boolean containsAny(String text, String[] needles) {
        if (text == null || needles == null) return false;
        for (String needle : needles) {
            if (needle != null && needle.length() > 0 && text.contains(needle.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String insertInlineImages(String content, List<WordpressMediaResult> images) {
        if (content == null || content.trim().isEmpty() || images == null || images.isEmpty()) {
            return content;
        }
        String result = content;
        Pattern pattern = Pattern.compile("(<h2[^>]*>.*?</h2>)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        List<Integer> insertPositions = new ArrayList<Integer>();
        while (matcher.find() && insertPositions.size() < images.size()) {
            String title = stripHtml(matcher.group(1));
            if (isGoodImageSectionTitle(title)) {
                insertPositions.add(matcher.end());
            }
        }

        if (insertPositions.isEmpty()) {
            return buildImageFigure(images.get(0)) + "\n" + result;
        }

        for (int i = insertPositions.size() - 1; i >= 0; i--) {
            WordpressMediaResult media = images.get(i);
            String figure = "\n" + buildImageFigure(media) + "\n";
            int position = insertPositions.get(i);
            result = result.substring(0, position) + figure + result.substring(position);
        }
        return result;
    }

    private String buildImageFigure(WordpressMediaResult media) {
        String sourceUrl = escapeHtml(media.getSourceUrl().trim());
        String alt = escapeHtml(emptyToDefault(media.getAltText(), "본문 이해를 돕는 이미지"));
        return "<figure class=\"wp-block-image size-large autoblog-inline-image\">"
                + "<img src=\"" + sourceUrl + "\" alt=\"" + alt + "\" loading=\"lazy\"/>"
                + "</figure>";
    }


    private String buildImageCaption(WordpressMediaResult media) {
        if (media == null || media.getExternalSourceUrl() == null || media.getExternalSourceUrl().trim().isEmpty()) {
            return "";
        }
        String caption = escapeHtml(emptyToDefault(media.getCaption(), "공식 출처 참고 이미지"));
        String sourceUrl = escapeHtml(media.getExternalSourceUrl().trim());
        String sourceTitle = escapeHtml(emptyToDefault(media.getExternalSourceTitle(), "공식 출처"));
        return "<figcaption>" + caption + " 출처: <a href=\"" + sourceUrl + "\" target=\"_blank\" rel=\"nofollow noopener\">" + sourceTitle + "</a></figcaption>";
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private Long resolveCategoryIdBeforePublish(WordpressSite site, GeneratedDraft draft) {
        if (draft.getWordpressCategoryId() != null && draft.getWordpressCategoryId().longValue() > 0) {
            return draft.getWordpressCategoryId();
        }
        if (wordpressPostService == null || draft.getCategories() == null || draft.getCategories().trim().isEmpty()) {
            return null;
        }
        try {
            List<WordpressCategory> wordpressCategories = wordpressPostService.getCategories(site.getId());
            String[] draftCategories = draft.getCategories().split(",");
            for (String draftCategory : draftCategories) {
                String wanted = normalizeCategory(draftCategory);
                if (wanted.length() == 0) continue;
                for (WordpressCategory wordpressCategory : wordpressCategories) {
                    String name = normalizeCategory(wordpressCategory.getName());
                    String slug = normalizeCategory(wordpressCategory.getSlug());
                    if (wanted.equals(name) || wanted.equals(slug)) {
                        return wordpressCategory.getId();
                    }
                }
            }
            System.err.println("[AutoBlog draft] wordpress category id not found. draftId=" + draft.getId() + " categories=" + draft.getCategories());
        } catch (Exception e) {
            System.err.println("[AutoBlog draft] wordpress category lookup failed before publish: " + e.getMessage());
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

    private void assertUniqueDraftTopic(long siteId, GeneratedDraft draft) {
        String title = emptyToDefault(draft.getTitle(), "").trim();
        String keyword = emptyToDefault(draft.getKeyword(), "").trim();
        if (title.length() < 4 && keyword.length() < 4) return;

        String normalizedTitle = normalizeForSimilarity(title);
        String normalizedKeyword = normalizeForSimilarity(keyword);
        Set<String> topicTokens = topicTokens(title + " " + keyword);

        if (blogPostDao != null) {
            List<BlogPost> posts = blogPostDao.findPosts(siteId, null, "all", "all", 80, 0);
            for (BlogPost post : posts) {
                if (post == null) continue;
                String existingTitle = emptyToDefault(post.getTitle(), "");
                if (isSameTopic(normalizedTitle, normalizedKeyword, topicTokens, existingTitle)) {
                    throw new IllegalStateException("비슷한 발행 글이 이미 있습니다: " + existingTitle);
                }
            }
        }

        List<GeneratedDraft> drafts = generatedDraftDao.findBySite(siteId);
        for (GeneratedDraft existing : drafts) {
            if (existing == null) continue;
            String existingTitle = emptyToDefault(existing.getTitle(), "");
            if (isSameTopic(normalizedTitle, normalizedKeyword, topicTokens, existingTitle + " " + existing.getKeyword())) {
                throw new IllegalStateException("비슷한 검토 대기 글이 이미 있습니다: " + existingTitle);
            }
        }
    }

    private boolean isSameTopic(String normalizedTitle, String normalizedKeyword, Set<String> topicTokens, String existing) {
        String normalizedExisting = normalizeForSimilarity(existing);
        if (normalizedExisting.length() < 4) return false;
        if (normalizedTitle.length() >= 4 && normalizedTitle.equals(normalizedExisting)) return true;
        if (normalizedKeyword.length() >= 4 && normalizedExisting.contains(normalizedKeyword)) return true;
        if (normalizedTitle.length() >= 4 && (normalizedTitle.contains(normalizedExisting) || normalizedExisting.contains(normalizedTitle))) return true;
        if (sharesTopicToken(topicTokens, existing)) return true;
        return normalizedTitle.length() >= 4 && bigramJaccard(normalizedTitle, normalizedExisting) >= 0.62d;
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

    private boolean sharesTopicToken(Set<String> tokens, String existing) {
        if (tokens == null || tokens.isEmpty()) return false;
        Set<String> existingTokens = topicTokens(existing);
        for (String token : tokens) {
            if (existingTokens.contains(token)) return true;
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
        String[] parts = text.split("\\s+");
        for (String part : parts) {
            part = normalizeTopicToken(part);
            if (isMeaningfulTopicToken(part)) result.add(part);
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

    private String applyTitleSuffix(String title, String suffix) {
        if (suffix == null || suffix.trim().isEmpty()) return title;
        if (title != null && title.endsWith(suffix.trim())) return title;
        return title + " " + suffix.trim();
    }

    private String applyMetaTemplate(String excerpt, GeneratedDraft draft, String template) {
        if (template == null || template.trim().isEmpty()) return excerpt;
        String value = template.replace("{title}", emptyToDefault(draft.getTitle(), ""))
                .replace("{keyword}", emptyToDefault(draft.getKeyword(), ""))
                .replace("{excerpt}", emptyToDefault(excerpt, ""));
        return value.trim().isEmpty() ? excerpt : value;
    }

    private String appendInternalLinks(long siteId, String content) {
        if (content == null) return "";
        // 자동 생성 본문이 이미 관련 글 섹션을 갖고 있거나,
        // 발행 흐름을 여러 번 탔을 때 내부 링크 섹션이 반복되는 것을 방지합니다.
        if (content.contains("함께 읽으면 좋은 글")
                || content.contains("함께 읽으면 좋을 글")
                || content.contains("관련 글")
                || content.contains("추천 글")) {
            return content;
        }
        java.util.List<BlogPost> posts = blogPostDao == null ? java.util.Collections.<BlogPost>emptyList() : blogPostDao.findPopularPosts(siteId);
        if (posts.isEmpty()) return content;
        StringBuilder links = new StringBuilder();
        int count = 0;
        for (BlogPost post : posts) {
            if (post.getWordpressUrl() == null || post.getWordpressUrl().trim().isEmpty()) continue;
            if (count == 0) {
                links.append("<h2>함께 읽으면 좋은 글</h2><ul>");
            }
            links.append("<li><a href=\"").append(post.getWordpressUrl()).append("\">")
                    .append(post.getTitle()).append("</a></li>");
            count++;
            if (count >= 3) break;
        }
        if (count == 0) return content;
        links.append("</ul>");
        return content + "\n" + links.toString();
    }
}
