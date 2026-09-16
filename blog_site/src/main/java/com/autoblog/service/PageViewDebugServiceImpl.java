package com.autoblog.service;

import com.autoblog.dao.BlogPostDao;
import com.autoblog.model.BlogPost;
import com.autoblog.model.PageViewMatchRow;
import com.autoblog.model.PageViewMetric;

import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PageViewDebugServiceImpl implements PageViewDebugService {
    private BlogPostDao blogPostDao;
    private WordpressSiteService wordpressSiteService;
    private Ga4AnalyticsService ga4AnalyticsService;

    public void setBlogPostDao(BlogPostDao blogPostDao) { this.blogPostDao = blogPostDao; }
    public void setWordpressSiteService(WordpressSiteService wordpressSiteService) { this.wordpressSiteService = wordpressSiteService; }
    public void setGa4AnalyticsService(Ga4AnalyticsService ga4AnalyticsService) { this.ga4AnalyticsService = ga4AnalyticsService; }

    @Override
    public List<PageViewMatchRow> getMatches() {
        long siteId = wordpressSiteService.getActiveSite().getId();
        List<BlogPost> posts = blogPostDao.findPosts(siteId, null, "all", "all", 500, 0);
        List<PageViewMetric> metrics = ga4AnalyticsService.getPageViews();
        Map<String, PageViewMetric> metricMap = new HashMap<String, PageViewMetric>();
        for (PageViewMetric metric : metrics) {
            metricMap.put(normalizePath(metric.getPagePath()), metric);
        }

        List<PageViewMatchRow> result = new ArrayList<PageViewMatchRow>();
        for (BlogPost post : posts) {
            String wordpressPath = normalizePath(post.getWordpressUrl());
            PageViewMetric metric = metricMap.get(wordpressPath);
            result.add(new PageViewMatchRow(
                    post.getTitle(),
                    post.getWordpressUrl(),
                    wordpressPath,
                    metric == null ? "" : metric.getPagePath(),
                    metric == null ? 0 : metric.getViews(),
                    metric != null));
        }
        return result;
    }

    private String normalizePath(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "/";
        try {
            String path = raw.startsWith("http://") || raw.startsWith("https://")
                    ? new URL(raw).getPath()
                    : raw;
            path = URLDecoder.decode(path, "UTF-8");
            if (path == null || path.trim().isEmpty()) return "/";
            if (path.endsWith("/") && path.length() > 1) path = path.substring(0, path.length() - 1);
            return path;
        } catch (Exception e) {
            return raw;
        }
    }
}
