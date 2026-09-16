package com.autoblog.service;

import com.autoblog.model.WordpressCategory;
import com.autoblog.model.WordpressMediaResult;
import com.autoblog.model.WordpressPostResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WordpressPostServiceImpl implements WordpressPostService {
    private WordpressSiteService wordpressSiteService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<Long, CachedCategories> categoryCache = Collections.synchronizedMap(new HashMap<Long, CachedCategories>());
    private static final long CATEGORY_CACHE_MILLIS = 10L * 60L * 1000L;
    private static final String USER_AGENT = "AutoBlog/1.0 (+https://growcha.com; WordPress REST API client)";

    public void setWordpressSiteService(WordpressSiteService wordpressSiteService) {
        this.wordpressSiteService = wordpressSiteService;
    }

    @Override
    public List<WordpressCategory> getCategories(long siteId) {
        try {
            CachedCategories cached = categoryCache.get(siteId);
            if (cached != null && !cached.isExpired()) {
                return new ArrayList<WordpressCategory>(cached.categories);
            }

            com.autoblog.model.WordpressSite site = wordpressSiteService.getSite(siteId);
            URL url = new URL(buildCategoriesApiUrl(site.getPostsApiUrl()));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("Accept", "application/json");
            applyCommonHeaders(connection);

            int code = connection.getResponseCode();
            String response = readBody(code >= 400 ? connection.getErrorStream() : connection.getInputStream());
            if (code == 401 || code == 403) {
                // 카테고리 목록은 보통 공개 API라 인증 없이도 조회됩니다.
                // 다만 일부 사이트가 REST 접근을 제한할 수 있어 인증 재시도를 한 번 수행합니다.
                connection.disconnect();
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(10000);
                connection.setRequestProperty("Accept", "application/json");
                applyCommonHeaders(connection);
                connection.setRequestProperty("Authorization", "Basic " + basicToken(site));
                code = connection.getResponseCode();
                response = readBody(code >= 400 ? connection.getErrorStream() : connection.getInputStream());
            }
            if (code < 200 || code >= 300) {
                throw new IllegalStateException("워드프레스 카테고리 조회 실패: HTTP " + code + " / " + trimForLog(response));
            }

            JsonNode root = objectMapper.readTree(response);
            List<WordpressCategory> categories = new ArrayList<WordpressCategory>();
            if (root.isArray()) {
                for (JsonNode item : root) {
                    WordpressCategory category = new WordpressCategory();
                    category.setId(item.path("id").asLong());
                    category.setName(item.path("name").asText());
                    category.setSlug(item.path("slug").asText());
                    category.setCount(item.path("count").asInt());
                    categories.add(category);
                }
            }
            categoryCache.put(siteId, new CachedCategories(categories));
            return categories;
        } catch (Exception e) {
            throw new IllegalStateException("워드프레스 카테고리 조회에 실패했습니다: " + rootMessage(e), e);
        }
    }

    @Override
    public boolean testConnection(long siteId) {
        return !getCategories(siteId).isEmpty();
    }

    @Override
    public WordpressPostResult createPost(long siteId, String title, String slug, String excerpt, String content, String status, Long categoryId) {
        return createPost(siteId, title, slug, excerpt, content, status, categoryId, null, null);
    }

    @Override
    public WordpressPostResult createPost(long siteId, String title, String slug, String excerpt, String content, String status, Long categoryId, Long featuredMediaId, String featuredMediaUrl) {
        try {
            com.autoblog.model.WordpressSite site = wordpressSiteService.getSite(siteId);
            URL url = new URL(site.getPostsApiUrl());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Authorization", "Basic " + basicToken(site));
            applyCommonHeaders(connection);
            // WordPress REST API의 categories는 배열 형태가 가장 안전합니다.
            // form-urlencoded 방식에서는 카테고리가 Uncategorized로 들어가는 경우가 있어 JSON으로 전송합니다.
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("title", title == null ? "" : title);
            payload.put("slug", slug == null ? "" : slug);
            payload.put("excerpt", excerpt == null ? "" : excerpt);
            payload.put("content", content == null ? "" : content);
            payload.put("status", status == null ? "draft" : status);
            if (categoryId != null && categoryId.longValue() > 0) {
                payload.putArray("categories").add(categoryId.longValue());
            }
            if (featuredMediaId != null && featuredMediaId.longValue() > 0) {
                payload.put("featured_media", featuredMediaId.longValue());
            }
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(objectMapper.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8));
            }
            int code = connection.getResponseCode();
            String response = readBody(code >= 400 ? connection.getErrorStream() : connection.getInputStream());
            if (code < 200 || code >= 300) {
                throw new IllegalStateException("워드프레스 글 생성 실패: HTTP " + code + " / " + response);
            }
            JsonNode created = objectMapper.readTree(response);
            // 생성 직후 대시보드/목록에 즉시 반영하기 위해 WordPress가 반환한 id/link를 돌려줍니다.
            return new WordpressPostResult(
                    created.path("id").asLong(),
                    created.path("link").asText(""),
                    created.path("status").asText(status),
                    featuredMediaId,
                    featuredMediaUrl);
        } catch (Exception e) {
            throw new IllegalStateException("워드프레스 글 생성에 실패했습니다.", e);
        }
    }

    @Override
    public WordpressMediaResult uploadMedia(long siteId, byte[] bytes, String filename, String altText) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("업로드할 이미지가 비어 있습니다.");
        }
        try {
            com.autoblog.model.WordpressSite site = wordpressSiteService.getSite(siteId);
            URL url = new URL(buildMediaApiUrl(site.getPostsApiUrl()));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(60000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Authorization", "Basic " + basicToken(site));
            applyCommonHeaders(connection);
            String uploadFilename = safeFilename(filename);
            connection.setRequestProperty("Content-Type", contentTypeForFilename(uploadFilename));
            connection.setRequestProperty("Content-Disposition", "attachment; filename=\"" + uploadFilename + "\"");

            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(bytes);
            }

            int code = connection.getResponseCode();
            String response = readBody(code >= 400 ? connection.getErrorStream() : connection.getInputStream());
            if (code < 200 || code >= 300) {
                throw new IllegalStateException("워드프레스 미디어 업로드 실패: HTTP " + code + " / " + trimForLog(response));
            }
            JsonNode created = objectMapper.readTree(response);
            long id = created.path("id").asLong();
            String sourceUrl = created.path("source_url").asText("");

            if (id > 0 && altText != null && !altText.trim().isEmpty()) {
                updateMediaAltText(site, id, altText);
            }

            return new WordpressMediaResult(id, sourceUrl, altText);
        } catch (Exception e) {
            throw new IllegalStateException("워드프레스 미디어 업로드에 실패했습니다: " + rootMessage(e), e);
        }
    }

    @Override
    public void deletePost(long siteId, long wordpressPostId) {
        try {
            com.autoblog.model.WordpressSite site = wordpressSiteService.getSite(siteId);
            URL url = new URL(site.getPostsApiUrl() + "/" + wordpressPostId);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("DELETE");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("Authorization", "Basic " + basicToken(site));
            applyCommonHeaders(connection);

            int code = connection.getResponseCode();
            String response = readBody(code >= 400 ? connection.getErrorStream() : connection.getInputStream());
            if (code == 404 || code == 410) {
                // 이미 워드프레스에서 삭제된 글이면 성공으로 간주합니다.
                return;
            }
            if (code < 200 || code >= 300) {
                throw new IllegalStateException("워드프레스 삭제 실패: HTTP " + code + " / " + response);
            }
        } catch (Exception e) {
            throw new IllegalStateException("워드프레스 글 삭제에 실패했습니다.", e);
        }
    }

    @Override
    public void updatePost(long siteId, long wordpressPostId, String title, String content, String status) {
        try {
            com.autoblog.model.WordpressSite site = wordpressSiteService.getSite(siteId);
            URL url = new URL(site.getPostsApiUrl() + "/" + wordpressPostId);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Authorization", "Basic " + basicToken(site));
            applyCommonHeaders(connection);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

            String body = "title=" + encode(title)
                    + "&content=" + encode(content)
                    + "&status=" + encode(status);
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new IllegalStateException("워드프레스 수정 실패: HTTP " + code);
            }
        } catch (Exception e) {
            throw new IllegalStateException("워드프레스 글 수정에 실패했습니다.", e);
        }
    }

    private String basicToken(com.autoblog.model.WordpressSite site) {
        // WordPress Application Password는 공백을 포함해 표시되므로 인증 헤더 생성 시 공백을 제거합니다.
        String raw = site.getUsername() + ":" + site.getApplicationPassword().replace(" ", "");
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private void applyCommonHeaders(HttpURLConnection connection) {
        // Cloudflare/보안 플러그인이 Java 기본 User-Agent 요청을 봇으로 차단하는 경우가 있어
        // 워드프레스 REST API 호출임을 명확히 식별할 수 있는 헤더를 공통으로 붙입니다.
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Accept", "application/json, text/plain, */*");
    }

    private String buildCategoriesApiUrl(String postsApiUrl) {
        String base = postsApiUrl == null ? "" : postsApiUrl.trim();
        int queryIndex = base.indexOf('?');
        if (queryIndex >= 0) {
            base = base.substring(0, queryIndex);
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.endsWith("/posts")) {
            base = base.substring(0, base.length() - "/posts".length()) + "/categories";
        } else if (!base.endsWith("/categories")) {
            base = base + "/categories";
        }
        return base + "?per_page=100&hide_empty=false";
    }

    private String buildMediaApiUrl(String postsApiUrl) {
        String base = postsApiUrl == null ? "" : postsApiUrl.trim();
        int queryIndex = base.indexOf('?');
        if (queryIndex >= 0) {
            base = base.substring(0, queryIndex);
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.endsWith("/posts")) {
            base = base.substring(0, base.length() - "/posts".length()) + "/media";
        } else if (!base.endsWith("/media")) {
            base = base + "/media";
        }
        return base;
    }

    private void updateMediaAltText(com.autoblog.model.WordpressSite site, long mediaId, String altText) {
        try {
            URL url = new URL(buildMediaApiUrl(site.getPostsApiUrl()) + "/" + mediaId);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Authorization", "Basic " + basicToken(site));
            applyCommonHeaders(connection);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("alt_text", altText);
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(objectMapper.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8));
            }

            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                String response = readBody(connection.getErrorStream());
                System.err.println("[AutoBlog image] alt_text 업데이트 실패: HTTP " + code + " / " + trimForLog(response));
            }
        } catch (Exception e) {
            System.err.println("[AutoBlog image] alt_text 업데이트 실패: " + e.getMessage());
        }
    }

    private String safeFilename(String filename) {
        String value = filename == null ? "autoblog-image.png" : filename.trim();
        if (value.length() == 0) value = "autoblog-image.png";
        value = value.replace('\\', '-').replace('/', '-').replace(':', '-').replace('*', '-')
                .replace('?', '-').replace('"', '-').replace('<', '-').replace('>', '-').replace('|', '-');
        String lower = value.toLowerCase();
        if (!(lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp") || lower.endsWith(".gif"))) {
            value = value + ".png";
        }
        return value;
    }

    private String contentTypeForFilename(String filename) {
        String lower = filename == null ? "" : filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        return "image/png";
    }


    private String trimForLog(String body) {
        if (body == null) return "";
        String compact = body.replace("\r", " ").replace("\n", " ").trim();
        return compact.length() > 500 ? compact.substring(0, 500) + "..." : compact;
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        String message = throwable.getMessage();
        while (current.getCause() != null) {
            current = current.getCause();
            if (current.getMessage() != null && !current.getMessage().trim().isEmpty()) {
                message = current.getMessage();
            }
        }
        return message == null ? throwable.getClass().getSimpleName() : message;
    }

    private String encode(String value) throws Exception {
        return URLEncoder.encode(value == null ? "" : value, "UTF-8");
    }

    private String readBody(InputStream inputStream) throws Exception {
        if (inputStream == null) {
            return "";
        }
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        return body.toString();
    }

    private static class CachedCategories {
        private final List<WordpressCategory> categories;
        private final long cachedAt;

        private CachedCategories(List<WordpressCategory> categories) {
            this.categories = new ArrayList<WordpressCategory>(categories);
            this.cachedAt = System.currentTimeMillis();
        }

        private boolean isExpired() {
            return System.currentTimeMillis() - cachedAt > CATEGORY_CACHE_MILLIS;
        }
    }
}
