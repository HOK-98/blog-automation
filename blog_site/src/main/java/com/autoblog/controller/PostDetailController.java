package com.autoblog.controller;

import com.autoblog.model.BlogPost;
import com.autoblog.service.DashboardService;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class PostDetailController implements Controller {
    private DashboardService dashboardService;

    public void setDashboardService(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) {
        long id = Long.parseLong(request.getParameter("id"));
        BlogPost post = dashboardService.getPostDetail(id);

        ModelAndView mav = new ModelAndView("post-detail");
        mav.addObject("post", post);
        mav.addObject("activeMenu", "posts");
        return mav;
    }
}
