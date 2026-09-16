package com.autoblog.dao;

import com.autoblog.model.GeneratedDraft;
import java.util.List;

public interface GeneratedDraftDao {
    void insert(GeneratedDraft draft);
    List<GeneratedDraft> findBySite(long siteId);
    List<GeneratedDraft> findAll();
    GeneratedDraft findById(long siteId, long id);
    void update(GeneratedDraft draft);
    void delete(long siteId, long id);
    void deleteByIds(long siteId, long[] ids);
}
