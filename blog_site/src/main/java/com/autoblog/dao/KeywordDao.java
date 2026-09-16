package com.autoblog.dao;

import com.autoblog.model.KeywordItem;
import java.util.List;

public interface KeywordDao {
    void insert(KeywordItem keyword);
    List<KeywordItem> findBySite(long siteId, String query, String category);
    int countBySite(long siteId);
    KeywordItem findById(long siteId, long id);
    int countExisting(long siteId, String category, String keyword);
    void deleteByIds(long siteId, long[] ids);
    void deleteByKeyword(long siteId, String keyword);
}
