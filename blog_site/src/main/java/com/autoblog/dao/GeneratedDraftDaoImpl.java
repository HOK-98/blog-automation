package com.autoblog.dao;

import com.autoblog.model.GeneratedDraft;
import org.apache.ibatis.session.SqlSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeneratedDraftDaoImpl implements GeneratedDraftDao {
    private SqlSession sqlSession;
    public void setSqlSession(SqlSession sqlSession) { this.sqlSession = sqlSession; }
    public void insert(GeneratedDraft draft) { sqlSession.insert("com.autoblog.mapper.GeneratedDraftMapper.insert", draft); }
    public List<GeneratedDraft> findBySite(long siteId) { return sqlSession.selectList("com.autoblog.mapper.GeneratedDraftMapper.findBySite", siteId); }
    public List<GeneratedDraft> findAll() { return sqlSession.selectList("com.autoblog.mapper.GeneratedDraftMapper.findAll"); }
    public GeneratedDraft findById(long siteId, long id) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("siteId", siteId);
        params.put("id", id);
        return sqlSession.selectOne("com.autoblog.mapper.GeneratedDraftMapper.findById", params);
    }
    public void update(GeneratedDraft draft) { sqlSession.update("com.autoblog.mapper.GeneratedDraftMapper.update", draft); }
    public void delete(long siteId, long id) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("siteId", siteId);
        params.put("id", id);
        sqlSession.delete("com.autoblog.mapper.GeneratedDraftMapper.delete", params);
    }
    public void deleteByIds(long siteId, long[] ids) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("siteId", siteId);
        params.put("ids", ids);
        sqlSession.delete("com.autoblog.mapper.GeneratedDraftMapper.deleteByIds", params);
    }
}
