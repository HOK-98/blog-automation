package com.autoblog.model;

import java.util.List;
import java.util.Map;

public class StatisticsData {
    private final int totalPosts;
    private final int publishedPosts;
    private final int draftPosts;
    private final List<DailyCount> dailyPublishedCounts;
    private final int maxDailyPublishedCount;
    private final Map<String, Integer> categoryCounts;
    private final List<BlogPost> popularPosts;
    private final Ga4Metrics ga4Metrics;

    public StatisticsData(int totalPosts, int publishedPosts, int draftPosts,
                          List<DailyCount> dailyPublishedCounts,
                          Map<String, Integer> categoryCounts,
                          List<BlogPost> popularPosts,
                          Ga4Metrics ga4Metrics) {
        this.totalPosts = totalPosts;
        this.publishedPosts = publishedPosts;
        this.draftPosts = draftPosts;
        this.dailyPublishedCounts = dailyPublishedCounts;
        this.maxDailyPublishedCount = calculateMaxDailyPublishedCount(dailyPublishedCounts);
        this.categoryCounts = categoryCounts;
        this.popularPosts = popularPosts;
        this.ga4Metrics = ga4Metrics;
    }

    public int getTotalPosts() { return totalPosts; }
    public int getPublishedPosts() { return publishedPosts; }
    public int getDraftPosts() { return draftPosts; }
    public List<DailyCount> getDailyPublishedCounts() { return dailyPublishedCounts; }
    public int getMaxDailyPublishedCount() { return maxDailyPublishedCount; }
    public Map<String, Integer> getCategoryCounts() { return categoryCounts; }
    public List<BlogPost> getPopularPosts() { return popularPosts; }
    public Ga4Metrics getGa4Metrics() { return ga4Metrics; }

    private int calculateMaxDailyPublishedCount(List<DailyCount> rows) {
        int max = 0;
        if (rows == null) {
            return max;
        }
        for (DailyCount row : rows) {
            if (row != null && row.getCount() > max) {
                max = row.getCount();
            }
        }
        return max;
    }
}
