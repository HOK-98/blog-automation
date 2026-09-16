package com.autoblog.controller;

import com.autoblog.service.PageViewDebugService;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class PageViewDebugController implements Controller {
    private PageViewDebugService pageViewDebugService;

    public void setPageViewDebugService(PageViewDebugService pageViewDebugService) {
        this.pageViewDebugService = pageViewDebugService;
    }

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) {
        ModelAndView mav = new ModelAndView("pageview-debug");
        mav.addObject("activeMenu", "statistics");
        mav.addObject("matches", pageViewDebugService.getMatches());
        return mav;
    }
}
