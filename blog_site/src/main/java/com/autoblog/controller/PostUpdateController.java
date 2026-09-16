package com.autoblog.controller;

import com.autoblog.model.BlogPost;
import com.autoblog.service.DashboardService;
import com.autoblog.service.WordpressPostService;
import com.autoblog.service.WordpressSyncService;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class PostUpdateController implements Controller {
    private DashboardService dashboardService;
    private WordpressPostService wordpressPostService;
    private WordpressSyncService wordpressSyncService;

    public void setDashboardService(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    public void setWordpressPostService(WordpressPostService wordpressPostService) {
        this.wordpressPostService = wordpressPostService;
    }

    public void setWordpressSyncService(WordpressSyncService wordpressSyncService) {
        this.wordpressSyncService = wordpressSyncService;
    }

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        long id = Long.parseLong(request.getParameter("id"));
        BlogPost post = dashboardService.getPostDetail(id);
        if (post != null && post.getWordpressPostId() != null) {
            wordpressPostService.updatePost(
                    post.getSiteId(),
                    post.getWordpressPostId(),
                    request.getParameter("title"),
                    request.getParameter("content"),
                    request.getParameter("status"));
            wordpressSyncService.syncPosts();
        }
        response.sendRedirect(request.getContextPath() + "/posts/detail?id=" + id);
        return null;
    }
}
