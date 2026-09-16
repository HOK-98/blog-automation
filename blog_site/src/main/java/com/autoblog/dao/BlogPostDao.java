package com.autoblog.dao;

import com.autoblog.model.BlogPost;
import com.autoblog.model.CountRow;
import com.autoblog.model.DailyCount;

import java.util.List;

public interface BlogPostDao {
    List<BlogPost> findPosts(long siteId, String keyword, String category, String status, int limit, int offset);

    int countPosts(long siteId, String keyword, String category, String status);

    List<CountRow> countByCategory(long siteId);

    List<CountRow> countByStatus(long siteId);

    List<BlogPost> findPopularPosts(long siteId);

    BlogPost findPostById(long siteId, long id);

    void upsertWordpressPost(BlogPost blogPost);

    void deleteById(long id);

    List<DailyCount> countPublishedByDay(long siteId);
}
