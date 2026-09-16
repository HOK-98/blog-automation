package com.autoblog.dao;

import com.autoblog.model.WordpressSite;
import java.util.List;

public interface WordpressSiteDao {
    List<WordpressSite> findAll();
    WordpressSite findActive();
    WordpressSite findById(long id);
    void insert(WordpressSite site);
    void update(WordpressSite site);
    void updateLastScheduledRunDate(long id);
    void activate(long id);
    void deactivateAll();
}
