package com.autoblog.controller;

import com.autoblog.model.DashboardData;
import com.autoblog.service.DashboardService;
import com.autoblog.service.StatisticsService;
import com.autoblog.service.WordpressSiteService;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HomeDashboardController implements Controller {
    private DashboardService dashboardService;
    private StatisticsService statisticsService;
    private WordpressSiteService wordpressSiteService;

    public void setDashboardService(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }
    public void setStatisticsService(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }
    public void setWordpressSiteService(WordpressSiteService wordpressSiteService) { this.wordpressSiteService = wordpressSiteService; }

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) {
        DashboardData dashboard = dashboardService.getDashboard(null, "all", "all", 1, 6);
        ModelAndView mav = new ModelAndView("home-dashboard");
        mav.addObject("dashboard", dashboard);
        mav.addObject("statistics", statisticsService.getLocalStatistics());
        mav.addObject("sites", wordpressSiteService.getSites());
        mav.addObject("activeMenu", "dashboard");
        return mav;
    }
}
