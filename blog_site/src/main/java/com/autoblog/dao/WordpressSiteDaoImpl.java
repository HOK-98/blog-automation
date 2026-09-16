package com.autoblog.dao;

import com.autoblog.model.WordpressSite;
import org.apache.ibatis.session.SqlSession;
import java.util.List;

public class WordpressSiteDaoImpl implements WordpressSiteDao {
    private SqlSession sqlSession;
    public void setSqlSession(SqlSession sqlSession) { this.sqlSession = sqlSession; }
    public List<WordpressSite> findAll() { return sqlSession.selectList("com.autoblog.mapper.WordpressSiteMapper.findAll"); }
    public WordpressSite findActive() { return sqlSession.selectOne("com.autoblog.mapper.WordpressSiteMapper.findActive"); }
    public WordpressSite findById(long id) { return sqlSession.selectOne("com.autoblog.mapper.WordpressSiteMapper.findById", id); }
    public void insert(WordpressSite site) { sqlSession.insert("com.autoblog.mapper.WordpressSiteMapper.insert", site); }
    public void update(WordpressSite site) { sqlSession.update("com.autoblog.mapper.WordpressSiteMapper.update", site); }
    public void updateLastScheduledRunDate(long id) { sqlSession.update("com.autoblog.mapper.WordpressSiteMapper.updateLastScheduledRunDate", id); }
    public void activate(long id) { sqlSession.update("com.autoblog.mapper.WordpressSiteMapper.activate", id); }
    public void deactivateAll() { sqlSession.update("com.autoblog.mapper.WordpressSiteMapper.deactivateAll"); }
}
