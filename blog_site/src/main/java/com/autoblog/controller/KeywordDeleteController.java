package com.autoblog.controller;

import com.autoblog.service.KeywordService;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class KeywordDeleteController implements Controller {
    private KeywordService keywordService;
    public void setKeywordService(KeywordService keywordService) { this.keywordService = keywordService; }
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String[] rawIds = request.getParameterValues("ids");
        if (rawIds != null && rawIds.length > 0) {
            long[] ids = new long[rawIds.length];
            for (int i = 0; i < rawIds.length; i++) ids[i] = Long.parseLong(rawIds[i]);
            keywordService.deleteKeywords(ids);
        }
        response.sendRedirect(request.getContextPath() + "/keywords/manage");
        return null;
    }
}
