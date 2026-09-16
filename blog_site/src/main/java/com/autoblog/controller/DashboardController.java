package com.autoblog.controller;

import com.autoblog.model.DashboardData;
import com.autoblog.service.DashboardService;
import com.autoblog.service.WordpressSiteService;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DashboardController implements Controller {
    private DashboardService dashboardService;
    private WordpressSiteService wordpressSiteService;

    public void setDashboardService(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }
    public void setWordpressSiteService(WordpressSiteService wordpressSiteService) { this.wordpressSiteService = wordpressSiteService; }

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) {
        String keyword = request.getParameter("q");
        String category = defaultValue(request.getParameter("category"), "all");
        String status = defaultValue(request.getParameter("status"), "all");
        int page = parsePage(request.getParameter("page"));

        DashboardData dashboard = dashboardService.getDashboard(keyword, category, status, page, 6);

        ModelAndView mav = new ModelAndView("post-list");
        mav.addObject("dashboard", dashboard);
        mav.addObject("q", keyword == null ? "" : keyword);
        mav.addObject("selectedCategory", category);
        mav.addObject("selectedStatus", status);
        mav.addObject("activeMenu", "posts");
        mav.addObject("sites", wordpressSiteService.getSites());
        return mav;
    }

    private int parsePage(String rawPage) {
        try {
            return Math.max(1, Integer.parseInt(rawPage));
        } catch (Exception ignored) {
            return 1;
        }
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
