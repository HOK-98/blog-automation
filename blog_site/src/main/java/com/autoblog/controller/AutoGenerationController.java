package com.autoblog.controller;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import com.autoblog.service.GenerationService;
import com.autoblog.service.GeneratedDraftService;
import com.autoblog.service.WordpressSiteService;
import com.autoblog.model.GeneratedDraft;
import com.autoblog.model.WordpressSite;
import com.autoblog.util.EncodingUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AutoGenerationController implements Controller {
    private GenerationService generationService;
    private GeneratedDraftService generatedDraftService;
    private WordpressSiteService wordpressSiteService;

    public void setGenerationService(GenerationService generationService) {
        this.generationService = generationService;
    }
    public void setGeneratedDraftService(GeneratedDraftService generatedDraftService) { this.generatedDraftService = generatedDraftService; }
    public void setWordpressSiteService(WordpressSiteService wordpressSiteService) { this.wordpressSiteService = wordpressSiteService; }

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) {
        ModelAndView mav = new ModelAndView("auto-generation");
        mav.addObject("activeMenu", "generation");
        String keyword = EncodingUtil.repairMojibake(request.getParameter("keyword"));
        String action = request.getParameter("action");
        if ("generate".equals(action) && keyword != null && !keyword.trim().isEmpty()) {
            WordpressSite activeSite = wordpressSiteService.getActiveSite();
            if (activeSite != null && !activeSite.isAutoContentGeneration()) {
                mav.addObject("error", "설정에서 자동 본문 생성이 꺼져 있습니다.");
                mav.addObject("keyword", keyword.trim());
                return mav;
            }
            com.autoblog.model.GenerationPreview preview = generationService.generatePreview(keyword.trim());
            GeneratedDraft savedDraft = generatedDraftService.savePreview(preview);
            mav.addObject("preview", preview);
            mav.addObject("savedDraft", savedDraft);
            if (activeSite != null && activeSite.isAutoPublishing() && preview.isApproved()) {
                generatedDraftService.publishDraft(savedDraft.getId());
                mav.addObject("autoPublished", true);
            }
        }
        mav.addObject("keyword", keyword == null ? "" : keyword.trim());
        return mav;
    }
}
