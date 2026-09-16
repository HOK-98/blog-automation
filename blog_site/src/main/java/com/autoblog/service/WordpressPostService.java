package com.autoblog.service;

import com.autoblog.model.WordpressCategory;
import com.autoblog.model.WordpressMediaResult;
import com.autoblog.model.WordpressPostResult;
import java.util.List;

public interface WordpressPostService {
    List<WordpressCategory> getCategories(long siteId);
    boolean testConnection(long siteId);
    WordpressPostResult createPost(long siteId, String title, String slug, String excerpt, String content, String status, Long categoryId);
    WordpressPostResult createPost(long siteId, String title, String slug, String excerpt, String content, String status, Long categoryId, Long featuredMediaId, String featuredMediaUrl);
    WordpressMediaResult uploadMedia(long siteId, byte[] bytes, String filename, String altText);
    void deletePost(long siteId, long wordpressPostId);

    void updatePost(long siteId, long wordpressPostId, String title, String content, String status);
}
