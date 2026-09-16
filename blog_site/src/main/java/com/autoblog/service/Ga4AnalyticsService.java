package com.autoblog.service;

import com.autoblog.model.Ga4Metrics;
import com.autoblog.model.PageViewMetric;
import java.util.List;

public interface Ga4AnalyticsService {
    Ga4Metrics getOverview();
    List<PageViewMetric> getPageViews();
}
