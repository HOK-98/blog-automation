package com.autoblog.service;

import com.autoblog.dao.BlogPostDao;
import com.autoblog.model.BlogPost;
import com.autoblog.model.PageViewMetric;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WordpressSyncServiceImpl implements WordpressSyncService {
    private BlogPostDao blogPostDao;
    private WordpressSiteService wordpressSiteService;
    private Ga4AnalyticsService ga4AnalyticsService;

    public void setBlogPostDao(BlogPostDao blogPostDao) {
        this.blogPostDao = blogPostDao;
    }

    public void setWordpressSiteService(WordpressSiteService wordpressSiteService) {
        this.wordpressSiteService = wordpressSiteService;
    }

    public void setGa4AnalyticsService(Ga4AnalyticsService ga4AnalyticsService) {
        this.ga4AnalyticsService = ga4AnalyticsService;
    }

    @Override
    public void syncPosts() {
        try {
            com.autoblog.model.WordpressSite site = wordpressSiteService.getActiveSite();
            if (site == null) {
                throw new IllegalStateException("활성 워드프레스 사이트가 없습니다.");
            }
            if (site.getPostsApiUrl() == null || site.getPostsApiUrl().trim().isEmpty()) {
                throw new IllegalStateException("워드프레스 posts API URL이 비어 있습니다.");
            }
            URL url = new URL(site.getPostsApiUrl() + "?per_page=100&_embed=1");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "AutoBlogAdmin/1.0");

            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                String errorBody = readBody(connection.getErrorStream());
                throw new IllegalStateException("워드프레스 API 응답 오류: HTTP " + status + " / " + errorBody);
            }

            StringBuilder json = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    json.append(line);
                }
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode posts = mapper.readTree(json.toString());
            Map<String, Long> pageViews = loadPageViewsSafely();
            for (JsonNode node : posts) {
                blogPostDao.upsertWordpressPost(toBlogPost(site.getId(), node, pageViews));
            }
        } catch (Exception e) {
            throw new IllegalStateException("워드프레스 글 동기화에 실패했습니다: " + rootMessage(e), e);
        }
    }

    private String readBody(InputStream inputStream) {
        if (inputStream == null) {
            return "";
        }
        try {
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }
            }
            return body.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private Map<String, Long> loadPageViewsSafely() {
        try {
            if (ga4AnalyticsService == null) {
                return new HashMap<String, Long>();
            }
            return toPageViewMap(ga4AnalyticsService.getPageViews());
        } catch (Exception e) {
            // 워드프레스 글 목록 동기화와 GA4 조회수 갱신은 분리해서 봅니다.
            // GA4가 잠깐 실패하더라도 글 목록/발행 상태 동기화까지 같이 실패시키지 않습니다.
            System.err.println("[AutoBlog sync] GA4 page views skipped: " + rootMessage(e));
            return new HashMap<String, Long>();
        }
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getName() : current.getMessage();
    }

    private BlogPost toBlogPost(long siteId, JsonNode node, Map<String, Long> pageViews) {
        BlogPost post = new BlogPost();
        post.setSiteId(siteId);
        post.setWordpressPostId(node.path("id").asLong());
        post.setTitle(stripHtml(node.path("title").path("rendered").asText()));
        post.setSummary(stripHtml(node.path("excerpt").path("rendered").asText()));
        post.setContent(node.path("content").path("rendered").asText());
        post.setCategory(resolveCategory(node));
        post.setStatus("publish".equals(node.path("status").asText()) ? "발행됨" : node.path("status").asText());
        post.setTags(resolveTags(node));
        post.setCoverImageUrl(resolveCoverImage(node));
        String wordpressUrl = node.path("link").asText("");
        post.setWordpressUrl(wordpressUrl);
        Long views = pageViews.get(normalizePath(wordpressUrl));
        post.setViewCount(views == null ? 0 : views);
        post.setPublishedAt(parseDate(node.path("date").asText()));
        post.setUpdatedAt(parseDate(node.path("modified").asText()));
        return post;
    }

    private String resolveCategory(JsonNode node) {
        JsonNode terms = node.path("_embedded").path("wp:term");
        if (terms.isArray()) {
            for (JsonNode group : terms) {
                for (JsonNode term : group) {
                    if ("category".equals(term.path("taxonomy").asText())) {
                        return term.path("name").asText("미분류");
                    }
                }
            }
        }
        return "미분류";
    }

    private String resolveTags(JsonNode node) {
        StringBuilder tags = new StringBuilder();
        JsonNode terms = node.path("_embedded").path("wp:term");
        if (terms.isArray()) {
            for (JsonNode group : terms) {
                for (JsonNode term : group) {
                    if ("post_tag".equals(term.path("taxonomy").asText())) {
                        if (tags.length() > 0) {
                            tags.append(" ");
                        }
                        tags.append("#").append(term.path("name").asText());
                    }
                }
            }
        }
        return tags.toString();
    }

    private String resolveCoverImage(JsonNode node) {
        JsonNode media = node.path("_embedded").path("wp:featuredmedia");
        if (media.isArray() && media.size() > 0) {
            return media.get(0).path("source_url").asText("");
        }
        return "";
    }

    private LocalDateTime parseDate(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private String stripHtml(String html) {
        if (html == null) {
            return "";
        }
        return html.replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Map<String, Long> toPageViewMap(List<PageViewMetric> metrics) {
        Map<String, Long> result = new HashMap<String, Long>();
        for (PageViewMetric metric : metrics) {
            result.put(normalizePath(metric.getPagePath()), metric.getViews());
        }
        return result;
    }

    private String normalizePath(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "/";
        }
        try {
            String path;
            if (raw.startsWith("http://") || raw.startsWith("https://")) {
                path = new URL(raw).getPath();
            } else {
                path = raw;
            }
            path = URLDecoder.decode(path, "UTF-8");
            if (path == null || path.trim().isEmpty()) {
                return "/";
            }
            return path.endsWith("/") && path.length() > 1
                    ? path.substring(0, path.length() - 1)
                    : path;
        } catch (Exception e) {
            return raw;
        }
    }
}
