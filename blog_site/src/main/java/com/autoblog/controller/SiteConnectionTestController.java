package com.autoblog.controller;

import com.autoblog.service.WordpressPostService;
import com.autoblog.service.WordpressSiteService;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SiteConnectionTestController implements Controller {
    private WordpressSiteService wordpressSiteService;
    private WordpressPostService wordpressPostService;

    public void setWordpressSiteService(WordpressSiteService wordpressSiteService) { this.wordpressSiteService = wordpressSiteService; }
    public void setWordpressPostService(WordpressPostService wordpressPostService) { this.wordpressPostService = wordpressPostService; }

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String result = "ok";
        try {
            wordpressPostService.testConnection(wordpressSiteService.getActiveSite().getId());
        } catch (Exception e) {
            result = "fail";
        }
        response.sendRedirect(request.getContextPath() + "/settings?test=" + result);
        return null;
    }
}
