package com.autoblog.controller;

import com.autoblog.service.GeneratedDraftService;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GeneratedDraftListController implements Controller {
    private GeneratedDraftService generatedDraftService;
    public void setGeneratedDraftService(GeneratedDraftService generatedDraftService) { this.generatedDraftService = generatedDraftService; }
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) {
        ModelAndView mav = new ModelAndView("generated-draft-list");
        mav.addObject("activeMenu", "generation");
        boolean allSites = "1".equals(request.getParameter("all"));
        Long siteId = null;
        if (request.getParameter("siteId") != null && !request.getParameter("siteId").trim().isEmpty()) {
            siteId = Long.parseLong(request.getParameter("siteId"));
        }
        mav.addObject("drafts", generatedDraftService.getDrafts(siteId, allSites));
        mav.addObject("allSites", allSites);
        mav.addObject("selectedSiteId", siteId);
        return mav;
    }
}
