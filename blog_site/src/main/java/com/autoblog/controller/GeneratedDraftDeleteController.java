package com.autoblog.controller;

import com.autoblog.service.GeneratedDraftService;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GeneratedDraftDeleteController implements Controller {
    private GeneratedDraftService generatedDraftService;
    public void setGeneratedDraftService(GeneratedDraftService generatedDraftService) { this.generatedDraftService = generatedDraftService; }
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String[] rawIds = request.getParameterValues("ids");
        if (rawIds != null && rawIds.length > 0) {
            generatedDraftService.deleteDrafts(parseIds(rawIds));
        } else if (request.getParameter("id") != null) {
            generatedDraftService.deleteDraft(Long.parseLong(request.getParameter("id")));
        }
        response.sendRedirect(request.getContextPath() + "/drafts");
        return null;
    }

    private long[] parseIds(String[] rawIds) {
        long[] ids = new long[rawIds.length];
        int count = 0;
        for (String rawId : rawIds) {
            try {
                ids[count++] = Long.parseLong(rawId);
            } catch (Exception ignored) {}
        }
        if (count == ids.length) return ids;
        long[] compact = new long[count];
        System.arraycopy(ids, 0, compact, 0, count);
        return compact;
    }
}
