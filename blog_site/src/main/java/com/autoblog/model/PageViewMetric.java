package com.autoblog.model;

public class PageViewMetric {
    private final String pagePath;
    private final long views;

    public PageViewMetric(String pagePath, long views) {
        this.pagePath = pagePath;
        this.views = views;
    }

    public String getPagePath() { return pagePath; }
    public long getViews() { return views; }
}
