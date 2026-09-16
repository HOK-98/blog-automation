package com.autoblog.model;

import java.util.List;
import java.util.Map;

public class DashboardData {
    private final List<BlogPost> posts;
    private final Map<String, Integer> categoryCounts;
    private final Map<String, Integer> statusCounts;
    private final List<BlogPost> popularPosts;
    private final int totalCount;
    private final int currentPage;
    private final int totalPages;

    public DashboardData(
            List<BlogPost> posts,
            Map<String, Integer> categoryCounts,
            Map<String, Integer> statusCounts,
            List<BlogPost> popularPosts,
            int totalCount,
            int currentPage,
            int totalPages) {
        this.posts = posts;
        this.categoryCounts = categoryCounts;
        this.statusCounts = statusCounts;
        this.popularPosts = popularPosts;
        this.totalCount = totalCount;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
    }

    public List<BlogPost> getPosts() { return posts; }
    public Map<String, Integer> getCategoryCounts() { return categoryCounts; }
    public Map<String, Integer> getStatusCounts() { return statusCounts; }
    public List<BlogPost> getPopularPosts() { return popularPosts; }
    public int getTotalCount() { return totalCount; }
    public int getCurrentPage() { return currentPage; }
    public int getTotalPages() { return totalPages; }
}
