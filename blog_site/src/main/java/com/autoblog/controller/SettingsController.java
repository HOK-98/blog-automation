package com.autoblog.controller;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import com.autoblog.model.WordpressSite;
import com.autoblog.service.WordpressPostService;
import com.autoblog.service.WordpressSiteService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SettingsController implements Controller {
    private WordpressSiteService wordpressSiteService;
    private WordpressPostService wordpressPostService;
    public void setWordpressSiteService(WordpressSiteService wordpressSiteService) { this.wordpressSiteService = wordpressSiteService; }
    public void setWordpressPostService(WordpressPostService wordpressPostService) { this.wordpressPostService = wordpressPostService; }
    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) {
        ModelAndView mav = new ModelAndView("settings");
        mav.addObject("activeMenu", "settings");
        mav.addObject("sites", wordpressSiteService.getSites());
        if ("1".equals(request.getParameter("new"))) {
            WordpressSite site = new WordpressSite();
            site.setTimezone("(UTC+09:00) Seoul");
            site.setLanguage("ko");
            site.setDefaultPostStatus("draft");
            site.setMetaDescriptionTemplate("{excerpt}");
            site.setAutoKeywordCollection(true);
            site.setAutoContentGeneration(true);
            site.setScheduledKeywordCount(5);
            site.setScheduledPostCount(1);
            site.setScheduleStartTime("09:00");
            site.setNotificationEnabled(true);
            site.setAutoImageGeneration(true);
            site.setAutoInternalLinks(true);
            mav.addObject("activeSite", site);
            mav.addObject("creatingNewSite", true);
        } else {
            WordpressSite activeSite = wordpressSiteService.getActiveSite();
            mav.addObject("activeSite", activeSite);
            addWordpressCategories(mav, activeSite);
        }
        return mav;
    }

    private void addWordpressCategories(ModelAndView mav, WordpressSite site) {
        if (site == null || wordpressPostService == null) return;
        try {
            mav.addObject("wordpressCategories", wordpressPostService.getCategories(site.getId()));
        } catch (Exception e) {
            mav.addObject("wordpressCategories", java.util.Collections.emptyList());
            mav.addObject("categoryLoadError", "워드프레스 카테고리를 불러오지 못했습니다.");
        }
    }
}
