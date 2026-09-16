package com.autoblog.service;

import com.autoblog.model.KeywordItem;
import java.util.List;
import com.autoblog.model.KeywordTrendData;

public interface KeywordService {
    List<KeywordItem> collectAndSave(String category, int count);
    List<KeywordItem> getKeywords(String query);
    List<KeywordItem> getKeywords(String query, String category);
    int getKeywordCount();
    KeywordItem getKeyword(long id);
    KeywordTrendData getTrend(String keyword);
    void deleteKeywords(long[] ids);
    void deleteKeywordAfterPublish(long siteId, String keyword);
}
