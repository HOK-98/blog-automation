package com.autoblog.service;

import com.autoblog.model.DashboardData;
import com.autoblog.model.BlogPost;

public interface DashboardService {
    DashboardData getDashboard(String keyword, String category, String status, int page, int pageSize);

    BlogPost getPostDetail(long id);

    void deleteLocalPost(long id);
}
