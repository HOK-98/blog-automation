package com.autoblog.controller;

import com.autoblog.model.WordpressSite;
import com.autoblog.service.WordpressSiteService;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SiteSaveController implements Controller {
    private WordpressSiteService wordpressSiteService;
    public void setWordpressSiteService(WordpressSiteService wordpressSiteService) { this.wordpressSiteService = wordpressSiteService; }
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        WordpressSite site = new WordpressSite();
        if (request.getParameter("id") != null && !request.getParameter("id").trim().isEmpty()) site.setId(Long.parseLong(request.getParameter("id")));
        site.setName(request.getParameter("name"));
        site.setDescription(request.getParameter("description"));
        site.setSiteUrl(request.getParameter("siteUrl"));
        site.setPostsApiUrl(request.getParameter("postsApiUrl"));
        site.setAdminEmail(request.getParameter("adminEmail"));
        site.setUsername(request.getParameter("username"));
        site.setApplicationPassword(request.getParameter("applicationPassword"));
        site.setTimezone(request.getParameter("timezone"));
        site.setLanguage(request.getParameter("language"));
        site.setDefaultPostStatus(request.getParameter("defaultPostStatus"));
        site.setSeoTitleSuffix(request.getParameter("seoTitleSuffix"));
        site.setMetaDescriptionTemplate(request.getParameter("metaDescriptionTemplate"));
        site.setAutoKeywordCollection(request.getParameter("autoKeywordCollection") != null);
        site.setAutoContentGeneration(request.getParameter("autoContentGeneration") != null);
        site.setAutoPublishing(request.getParameter("autoPublishing") != null);
        site.setScheduleEnabled(request.getParameter("scheduleEnabled") != null);
        site.setScheduleStartTime(request.getParameter("scheduleStartTime"));
        site.setScheduledKeywordCount(parseInt(request.getParameter("scheduledKeywordCount"), 5, 1, 20));
        site.setScheduledPostCount(parseInt(request.getParameter("scheduledPostCount"), 1, 1, 10));
        site.setScheduleCategory(request.getParameter("scheduleCategory"));
        site.setNotificationEnabled(request.getParameter("notificationEnabled") != null);
        site.setNotificationEmail(request.getParameter("notificationEmail"));
        site.setNotifyOnPublish(request.getParameter("notifyOnPublish") != null);
        site.setAutoImageGeneration(request.getParameter("autoImageGeneration") != null);
        site.setAutoInternalLinks(request.getParameter("autoInternalLinks") != null);
        wordpressSiteService.save(site);
        response.sendRedirect(request.getContextPath() + "/settings");
        return null;
    }

    private int parseInt(String raw, int fallback, int min, int max) {
        try {
            int value = Integer.parseInt(raw);
            return Math.max(min, Math.min(max, value));
        } catch (Exception e) {
            return fallback;
        }
    }
}
