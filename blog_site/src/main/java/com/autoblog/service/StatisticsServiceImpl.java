package com.autoblog.service;

import com.autoblog.dao.BlogPostDao;
import com.autoblog.model.CountRow;
import com.autoblog.model.Ga4Metrics;
import com.autoblog.model.StatisticsData;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StatisticsServiceImpl implements StatisticsService {
    private BlogPostDao blogPostDao;
    private Ga4AnalyticsService ga4AnalyticsService;
    private WordpressSiteService wordpressSiteService;

    public void setBlogPostDao(BlogPostDao blogPostDao) {
        this.blogPostDao = blogPostDao;
    }

    public void setGa4AnalyticsService(Ga4AnalyticsService ga4AnalyticsService) {
        this.ga4AnalyticsService = ga4AnalyticsService;
    }
    public void setWordpressSiteService(WordpressSiteService wordpressSiteService) { this.wordpressSiteService = wordpressSiteService; }

    @Override
    public StatisticsData getStatistics() {
        return buildStatistics(ga4AnalyticsService.getOverview());
    }

    @Override
    public StatisticsData getLocalStatistics() {
        return buildStatistics(new Ga4Metrics());
    }

    private StatisticsData buildStatistics(Ga4Metrics ga4Metrics) {
        long siteId = wordpressSiteService.getActiveSite().getId();
        Map<String, Integer> statusMap = toMap(blogPostDao.countByStatus(siteId));
        Map<String, Integer> categoryMap = toMap(blogPostDao.countByCategory(siteId));
        int published = valueOf(statusMap, "발행됨");
        int drafts = valueOf(statusMap, "임시저장");
        int total = 0;
        for (Integer value : statusMap.values()) {
            total += value;
        }
        return new StatisticsData(
                total,
                published,
                drafts,
                blogPostDao.countPublishedByDay(siteId),
                categoryMap,
                blogPostDao.findPopularPosts(siteId),
                ga4Metrics);
    }

    private Map<String, Integer> toMap(List<CountRow> rows) {
        Map<String, Integer> result = new LinkedHashMap<String, Integer>();
        for (CountRow row : rows) {
            result.put(row.getName(), row.getCount());
        }
        return result;
    }

    private int valueOf(Map<String, Integer> map, String key) {
        return map.containsKey(key) ? map.get(key) : 0;
    }
}
