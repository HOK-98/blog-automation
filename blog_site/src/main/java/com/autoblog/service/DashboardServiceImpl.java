package com.autoblog.service;

import com.autoblog.dao.BlogPostDao;
import com.autoblog.model.BlogPost;
import com.autoblog.model.CountRow;
import com.autoblog.model.DashboardData;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DashboardServiceImpl implements DashboardService {
    private BlogPostDao blogPostDao;
    private WordpressSyncService wordpressSyncService;
    private WordpressSiteService wordpressSiteService;

    public void setBlogPostDao(BlogPostDao blogPostDao) {
        this.blogPostDao = blogPostDao;
    }

    public void setWordpressSyncService(WordpressSyncService wordpressSyncService) {
        this.wordpressSyncService = wordpressSyncService;
    }
    public void setWordpressSiteService(WordpressSiteService wordpressSiteService) { this.wordpressSiteService = wordpressSiteService; }

    @Override
    public DashboardData getDashboard(String keyword, String category, String status, int page, int pageSize) {
        long siteId = wordpressSiteService.getActiveSite().getId();
        int safePage = Math.max(page, 1);
        int totalCount = blogPostDao.countPosts(siteId, keyword, category, status);
        int totalPages = Math.max(1, (int) Math.ceil(totalCount / (double) pageSize));
        safePage = Math.min(safePage, totalPages);

        List<BlogPost> posts = blogPostDao.findPosts(
                siteId,
                keyword,
                category,
                status,
                pageSize,
                (safePage - 1) * pageSize);

        return new DashboardData(
                posts,
                toCountMap(blogPostDao.countByCategory(siteId)),
                toCountMap(blogPostDao.countByStatus(siteId)),
                blogPostDao.findPopularPosts(siteId),
                totalCount,
                safePage,
                totalPages);
    }

    @Override
    public BlogPost getPostDetail(long id) {
        return blogPostDao.findPostById(wordpressSiteService.getActiveSite().getId(), id);
    }

    @Override
    public void deleteLocalPost(long id) {
        blogPostDao.deleteById(id);
    }

    private Map<String, Integer> toCountMap(List<CountRow> rows) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (CountRow row : rows) {
            result.put(row.getName(), row.getCount());
        }
        return result;
    }
}
