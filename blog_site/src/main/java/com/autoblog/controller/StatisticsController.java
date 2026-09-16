package com.autoblog.controller;

import com.autoblog.service.StatisticsService;
import com.autoblog.service.WordpressSiteService;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class StatisticsController implements Controller {
    private StatisticsService statisticsService;
    private WordpressSiteService wordpressSiteService;

    public void setStatisticsService(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }
    public void setWordpressSiteService(WordpressSiteService wordpressSiteService) { this.wordpressSiteService = wordpressSiteService; }

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) {
        ModelAndView mav = new ModelAndView("statistics");
        mav.addObject("activeMenu", "statistics");
        mav.addObject("statistics", statisticsService.getStatistics());
        mav.addObject("sites", wordpressSiteService.getSites());
        return mav;
    }
}
