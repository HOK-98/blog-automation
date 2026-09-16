package com.autoblog.controller;

import com.autoblog.service.WordpressSyncService;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WordpressSyncController implements Controller {
    private WordpressSyncService wordpressSyncService;

    public void setWordpressSyncService(WordpressSyncService wordpressSyncService) {
        this.wordpressSyncService = wordpressSyncService;
    }

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        wordpressSyncService.syncPosts();
        String redirect = request.getParameter("redirect");
        if (redirect == null || redirect.trim().isEmpty() || !redirect.startsWith("/")) {
            redirect = "/posts";
        }
        response.sendRedirect(request.getContextPath() + redirect);
        return null;
    }
}
