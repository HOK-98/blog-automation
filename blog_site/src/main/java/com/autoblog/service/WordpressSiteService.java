package com.autoblog.service;

import com.autoblog.model.WordpressSite;
import java.util.List;

public interface WordpressSiteService {
    List<WordpressSite> getSites();
    WordpressSite getActiveSite();
    WordpressSite getSite(long id);
    void save(WordpressSite site);
    void markScheduledRun(long id);
    void switchSite(long id);
}
