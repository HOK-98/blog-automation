package com.autoblog.controller;

import com.autoblog.service.GeneratedDraftService;
import com.autoblog.service.WordpressPostService;
import com.autoblog.service.WordpressSiteService;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;

public class GeneratedDraftEditController implements Controller {
    private GeneratedDraftService generatedDraftService;
    private WordpressPostService wordpressPostService;
    private WordpressSiteService wordpressSiteService;
    public void setGeneratedDraftService(GeneratedDraftService generatedDraftService) { this.generatedDraftService = generatedDraftService; }
    public void setWordpressPostService(WordpressPostService wordpressPostService) { this.wordpressPostService = wordpressPostService; }
    public void setWordpressSiteService(WordpressSiteService wordpressSiteService) { this.wordpressSiteService = wordpressSiteService; }
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) {
        ModelAndView mav = new ModelAndView("generated-draft-edit");
        mav.addObject("activeMenu", "generation");
        mav.addObject("draft", generatedDraftService.getDraft(Long.parseLong(request.getParameter("id"))));
        try {
            mav.addObject("wordpressCategories", wordpressPostService.getCategories(wordpressSiteService.getActiveSite().getId()));
        } catch (Exception e) {
            mav.addObject("wordpressCategories", Collections.emptyList());
            mav.addObject("categoryLoadError", "워드프레스 카테고리를 불러오지 못했습니다. 사이트 설정과 Application Password를 확인하세요.");
        }
        return mav;
    }
}
