package com.autoblog.controller;

import com.autoblog.service.WordpressSiteService;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SiteSwitchController implements Controller {
    private WordpressSiteService wordpressSiteService;
    public void setWordpressSiteService(WordpressSiteService wordpressSiteService) { this.wordpressSiteService = wordpressSiteService; }
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        wordpressSiteService.switchSite(Long.parseLong(request.getParameter("siteId")));
        response.sendRedirect(request.getHeader("Referer") == null ? request.getContextPath() + "/dashboard" : request.getHeader("Referer"));
        return null;
    }
}
