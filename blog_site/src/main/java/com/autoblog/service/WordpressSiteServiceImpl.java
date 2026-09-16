package com.autoblog.service;

import com.autoblog.dao.WordpressSiteDao;
import com.autoblog.model.WordpressSite;
import java.util.List;

public class WordpressSiteServiceImpl implements WordpressSiteService {
    private WordpressSiteDao wordpressSiteDao;
    
    public void setWordpressSiteDao(WordpressSiteDao wordpressSiteDao) { this.wordpressSiteDao = wordpressSiteDao; }
    
    public List<WordpressSite> getSites() { return wordpressSiteDao.findAll(); }
    
    public WordpressSite getActiveSite() { return wordpressSiteDao.findActive(); }
    
    public WordpressSite getSite(long id) { return wordpressSiteDao.findById(id); }
    
    public void save(WordpressSite site) {
        if (site.getId() == 0) {
            if (wordpressSiteDao.findAll().isEmpty()) {
                site.setActive(true);
            }
            wordpressSiteDao.insert(site);
        } else {
            wordpressSiteDao.update(site);
        }
    }
    public void markScheduledRun(long id) {
        wordpressSiteDao.updateLastScheduledRunDate(id);
    }
    public void switchSite(long id) {
        wordpressSiteDao.deactivateAll();
        wordpressSiteDao.activate(id);
    }
}
