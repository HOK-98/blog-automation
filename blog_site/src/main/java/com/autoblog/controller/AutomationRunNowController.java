package com.autoblog.controller;

import com.autoblog.service.ScheduledAutomationService;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AutomationRunNowController implements Controller {
    private ScheduledAutomationService scheduledAutomationService;

    public void setScheduledAutomationService(ScheduledAutomationService scheduledAutomationService) {
        this.scheduledAutomationService = scheduledAutomationService;
    }

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String result = "started";
        try {
            boolean started = scheduledAutomationService.runNowAsync();
            if (!started) {
                result = "running";
            }
        } catch (Exception e) {
            result = "fail";
            e.printStackTrace();
        }
        response.sendRedirect(request.getContextPath() + "/settings?runNow=" + result);
        return null;
    }
}
