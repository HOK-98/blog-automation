package com.autoblog.dao;

import com.autoblog.model.BlogPost;
import com.autoblog.model.CountRow;
import com.autoblog.model.DailyCount;
import org.apache.ibatis.session.SqlSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlogPostDaoImpl implements BlogPostDao {
    private SqlSession sqlSession;

    public void setSqlSession(SqlSession sqlSession) {
        this.sqlSession = sqlSession;
    }

    @Override
    public List<BlogPost> findPosts(long siteId, String keyword, String category, String status, int limit, int offset) {
        Map<String, Object> params = new HashMap<>();
        params.put("siteId", siteId);
        params.put("keyword", keyword);
        params.put("category", category);
        params.put("status", status);
        params.put("limit", limit);
        params.put("offset", offset);
        return sqlSession.selectList("com.autoblog.mapper.BlogPostMapper.findPosts", params);
    }

    @Override
    public int countPosts(long siteId, String keyword, String category, String status) {
        Map<String, Object> params = new HashMap<>();
        params.put("siteId", siteId);
        params.put("keyword", keyword);
        params.put("category", category);
        params.put("status", status);
        return sqlSession.selectOne("com.autoblog.mapper.BlogPostMapper.countPosts", params);
    }

    @Override
    public List<CountRow> countByCategory(long siteId) {
        return sqlSession.selectList("com.autoblog.mapper.BlogPostMapper.countByCategory", siteId);
    }

    @Override
    public List<CountRow> countByStatus(long siteId) {
        return sqlSession.selectList("com.autoblog.mapper.BlogPostMapper.countByStatus", siteId);
    }

    @Override
    public List<BlogPost> findPopularPosts(long siteId) {
        return sqlSession.selectList("com.autoblog.mapper.BlogPostMapper.findPopularPosts", siteId);
    }

    @Override
    public BlogPost findPostById(long siteId, long id) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("siteId", siteId);
        params.put("id", id);
        return sqlSession.selectOne("com.autoblog.mapper.BlogPostMapper.findPostById", params);
    }

    @Override
    public void upsertWordpressPost(BlogPost blogPost) {
        sqlSession.insert("com.autoblog.mapper.BlogPostMapper.upsertWordpressPost", blogPost);
    }

    @Override
    public void deleteById(long id) {
        sqlSession.delete("com.autoblog.mapper.BlogPostMapper.deleteById", id);
    }

    @Override
    public List<DailyCount> countPublishedByDay(long siteId) {
        return sqlSession.selectList("com.autoblog.mapper.BlogPostMapper.countPublishedByDay", siteId);
    }
}
