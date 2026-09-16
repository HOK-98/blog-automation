package com.autoblog.controller;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import com.autoblog.service.KeywordService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class KeywordAnalysisController implements Controller {
    private KeywordService keywordService;
    public void setKeywordService(KeywordService keywordService) { this.keywordService = keywordService; }
    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) {
        ModelAndView mav = new ModelAndView("keyword-analysis");
        mav.addObject("activeMenu", "keywords");
        String keyword = request.getParameter("keyword");
        mav.addObject("keyword", keyword == null ? "" : keyword);
        mav.addObject("relatedKeywords", keywordService.getKeywords(keyword));
        mav.addObject("trend", keywordService.getTrend(keyword));
        return mav;
    }
}
