package com.autoblog.controller;

import com.autoblog.model.GeneratedDraft;
import com.autoblog.service.GeneratedDraftService;
import com.autoblog.util.EncodingUtil;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GeneratedDraftPublishController implements Controller {
    private GeneratedDraftService generatedDraftService;
    public void setGeneratedDraftService(GeneratedDraftService generatedDraftService) { this.generatedDraftService = generatedDraftService; }
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        long id = Long.parseLong(request.getParameter("id"));
        if (request.getParameter("title") != null) {
            GeneratedDraft draft = generatedDraftService.getDraft(id);
            if (draft != null) {
                draft.setTitle(EncodingUtil.repairMojibake(request.getParameter("title")));
                draft.setSlug(EncodingUtil.repairMojibake(request.getParameter("slug")));
                draft.setExcerpt(EncodingUtil.repairMojibake(request.getParameter("excerpt")));
                draft.setContentHtml(EncodingUtil.repairMojibake(request.getParameter("contentHtml")));
                draft.setCategories(EncodingUtil.repairMojibake(request.getParameter("categories")));
                draft.setWordpressCategoryId(parseLong(request.getParameter("wordpressCategoryId")));
                draft.setStatus(EncodingUtil.repairMojibake(request.getParameter("status")));
                generatedDraftService.updateDraft(draft);
            }
        }
        generatedDraftService.publishDraft(id);
        response.sendRedirect(request.getContextPath() + "/drafts");
        return null;
    }

    private Long parseLong(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            long parsed = Long.parseLong(value);
            return parsed > 0 ? Long.valueOf(parsed) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
