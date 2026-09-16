package com.autoblog.service;

import com.autoblog.model.StatisticsData;

public interface StatisticsService {
    StatisticsData getStatistics();
    StatisticsData getLocalStatistics();
}
