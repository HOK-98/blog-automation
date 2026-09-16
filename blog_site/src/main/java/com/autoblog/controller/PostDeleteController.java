package com.autoblog.controller;

import com.autoblog.model.BlogPost;
import com.autoblog.service.DashboardService;
import com.autoblog.service.WordpressPostService;
import com.autoblog.service.WordpressSyncService;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class PostDeleteController implements Controller {
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
        if (post != null) {
            boolean wordpressDeleted = false;
            if (post.getWordpressPostId() != null && post.getWordpressPostId().longValue() > 0) {
                try {
                    wordpressPostService.deletePost(post.getSiteId(), post.getWordpressPostId());
                    wordpressDeleted = true;
                } catch (Exception e) {
                    System.err.println("[AutoBlog delete] WordPress delete failed. localId=" + id
                            + " wpId=" + post.getWordpressPostId() + " / " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // 워드프레스에서 이미 지워졌거나 REST API가 404를 돌려주는 경우에도
            // 관리자 목록에서는 사라져야 하므로 로컬 DB는 항상 정리합니다.
            dashboardService.deleteLocalPost(id);

            if (wordpressDeleted && wordpressSyncService != null) {
                try {
                    wordpressSyncService.syncPosts();
                } catch (Exception e) {
                    System.err.println("[AutoBlog delete] sync after delete skipped: " + e.getMessage());
                }
            }
        }
        response.sendRedirect(request.getContextPath() + "/posts");
        return null;
    }
}
