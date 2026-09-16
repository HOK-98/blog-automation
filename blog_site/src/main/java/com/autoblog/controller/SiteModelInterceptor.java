package com.autoblog.controller;

import com.autoblog.service.WordpressSiteService;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SiteModelInterceptor extends HandlerInterceptorAdapter {
    private WordpressSiteService wordpressSiteService;

    public void setWordpressSiteService(WordpressSiteService wordpressSiteService) {
        this.wordpressSiteService = wordpressSiteService;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        if (modelAndView != null) {
            modelAndView.addObject("sites", wordpressSiteService.getSites());
            modelAndView.addObject("activeSite", wordpressSiteService.getActiveSite());
        }
    }
}
