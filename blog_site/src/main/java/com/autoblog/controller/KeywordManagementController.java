package com.autoblog.controller;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import com.autoblog.service.KeywordService;
import com.autoblog.service.DashboardService;
import com.autoblog.service.WordpressSiteService;
import com.autoblog.model.KeywordItem;
import com.autoblog.model.WordpressSite;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public class KeywordManagementController implements Controller {
    private KeywordService keywordService;
    private DashboardService dashboardService;
    private WordpressSiteService wordpressSiteService;
    public void setKeywordService(KeywordService keywordService) { this.keywordService = keywordService; }
    public void setDashboardService(DashboardService dashboardService) { this.dashboardService = dashboardService; }
    public void setWordpressSiteService(WordpressSiteService wordpressSiteService) { this.wordpressSiteService = wordpressSiteService; }
    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if ("collect".equals(request.getParameter("action"))) {
            WordpressSite activeSite = wordpressSiteService.getActiveSite();
            if (activeSite != null && !activeSite.isAutoKeywordCollection()) {
                response.sendRedirect(request.getContextPath() + "/keywords/manage?disabled=keyword");
                return null;
            }
            int count = parseCount(request.getParameter("count"));
            int added = keywordService.collectAndSave(request.getParameter("category"), count).size();
            response.sendRedirect(request.getContextPath() + "/keywords/manage?added=" + added);
            return null;
        }
        ModelAndView mav = new ModelAndView("keyword-management");
        mav.addObject("activeMenu", "keywords");
        String category = request.getParameter("categoryFilter");
        List<KeywordItem> keywords = keywordService.getKeywords(request.getParameter("q"), category);
        mav.addObject("keywords", keywords);
        mav.addObject("keywordCount", keywordService.getKeywordCount());
        mav.addObject("q", request.getParameter("q") == null ? "" : request.getParameter("q"));
        mav.addObject("selectedCategory", category == null ? "all" : category);
        mav.addObject("dashboard", dashboardService.getDashboard(null, "all", "all", 1, 1));
        mav.addObject("added", request.getParameter("added"));
        mav.addObject("disabled", request.getParameter("disabled"));
        return mav;
    }

    private int parseCount(String raw) {
        try {
            return Math.max(1, Math.min(20, Integer.parseInt(raw)));
        } catch (Exception ignored) {
            return 5;
        }
    }

}
