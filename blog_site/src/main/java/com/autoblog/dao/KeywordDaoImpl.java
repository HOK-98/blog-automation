package com.autoblog.dao;

import com.autoblog.model.KeywordItem;
import org.apache.ibatis.session.SqlSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeywordDaoImpl implements KeywordDao {
    private SqlSession sqlSession;
    public void setSqlSession(SqlSession sqlSession) { this.sqlSession = sqlSession; }
    public void insert(KeywordItem keyword) { sqlSession.insert("com.autoblog.mapper.KeywordMapper.insert", keyword); }
    public List<KeywordItem> findBySite(long siteId, String query, String category) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("siteId", siteId);
        params.put("query", query);
        params.put("category", category);
        return sqlSession.selectList("com.autoblog.mapper.KeywordMapper.findBySite", params);
    }
    public int countBySite(long siteId) { return sqlSession.selectOne("com.autoblog.mapper.KeywordMapper.countBySite", siteId); }
    public KeywordItem findById(long siteId, long id) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("siteId", siteId);
        params.put("id", id);
        return sqlSession.selectOne("com.autoblog.mapper.KeywordMapper.findById", params);
    }
    public int countExisting(long siteId, String category, String keyword) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("siteId", siteId);
        params.put("category", category);
        params.put("keyword", keyword);
        return sqlSession.selectOne("com.autoblog.mapper.KeywordMapper.countExisting", params);
    }
    public void deleteByIds(long siteId, long[] ids) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("siteId", siteId);
        params.put("ids", ids);
        sqlSession.delete("com.autoblog.mapper.KeywordMapper.deleteByIds", params);
    }
    public void deleteByKeyword(long siteId, String keyword) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("siteId", siteId);
        params.put("keyword", keyword);
        sqlSession.delete("com.autoblog.mapper.KeywordMapper.deleteByKeyword", params);
    }
}
