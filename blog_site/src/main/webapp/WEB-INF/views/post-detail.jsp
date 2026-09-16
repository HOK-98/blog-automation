<%@ page import="java.time.format.DateTimeFormatter"%>
<%@ page import="com.autoblog.model.BlogPost"%>
<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<%
    BlogPost post = (BlogPost) request.getAttribute("post");
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title><%= post.getTitle() %></title>
<link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/detail.css">
</head>
<body>
    <div class="detail-shell">
        <jsp:include page="/WEB-INF/views/common/sidebar.jsp" />

        <main class="main-panel">
            <header class="detail-top">
                <a href="<%= request.getContextPath() %>/posts">‹ 블로그 목록으로 돌아가기</a>
                <div>
                    <button>‹ 이전 글</button>
                    <button>다음 글 ›</button>
                </div>
            </header>

            <section class="content-grid">
                <article class="article-card">
                    <div class="badges">
                        <span>발행됨</span>
                        <em><%= post.getCategory() %></em>
                    </div>
                    <h1><%= post.getTitle() %></h1>
                    <div class="meta">
                        <span>◉ 관리자</span>
                        <span>▣ <%= post.getPublishedAt() == null ? "-" : post.getPublishedAt().format(formatter) %></span>
                        <span>▤ <%= post.getCategory() %></span>
                        <span>◔ <%= String.format("%,d", post.getViewCount()) %></span>
                    </div>

                    <% if (post.getCoverImageUrl() != null && !post.getCoverImageUrl().trim().isEmpty()) { %>
                    <div class="hero-image" style="background-image:url('<%= post.getCoverImageUrl() %>')"></div>
                    <% } %>

                    <div class="wordpress-content">
                        <%= post.getContent() %>
                    </div>

                    <div class="tag-row">
                        <% if (post.getTags() != null && !post.getTags().trim().isEmpty()) {
                            String[] tags = post.getTags().split(" ");
                            for (String tag : tags) { %>
                                <span><%= tag %></span>
                        <%  }
                           } %>
                    </div>
                </article>

                <aside class="right-column">
                    <section class="info-card">
                        <div class="card-head"><h2>발행 정보</h2><b>발행됨</b></div>
                        <dl>
                            <div><dt>워드프레스 ID</dt><dd><%= post.getWordpressPostId() == null ? "-" : post.getWordpressPostId() %></dd></div>
                            <div><dt>발행일</dt><dd><%= post.getPublishedAt() == null ? "-" : post.getPublishedAt().format(formatter) %></dd></div>
                            <div><dt>최종 수정일</dt><dd><%= post.getUpdatedAt() == null ? "-" : post.getUpdatedAt().format(formatter) %></dd></div>
                            <div><dt>카테고리</dt><dd><%= post.getCategory() %></dd></div>
                            <div><dt>태그</dt><dd><%= post.getTags() == null || post.getTags().trim().isEmpty() ? "-" : post.getTags() %></dd></div>
                        </dl>
                        <% if (post.getWordpressUrl() != null && !post.getWordpressUrl().trim().isEmpty()) { %>
                        <a class="secondary-btn" href="<%= post.getWordpressUrl() %>" target="_blank">◉ 워드프레스에서 보기</a>
                        <% } %>
                        <a class="primary-btn" href="<%= request.getContextPath() %>/posts/edit?id=<%= post.getId() %>">✎ 글 수정하기</a>
                        <a class="danger-btn" href="<%= request.getContextPath() %>/posts/delete?id=<%= post.getId() %>"
                           onclick="return confirm('이 글을 워드프레스에서 삭제할까요?');">▣ 글 삭제하기</a>
                    </section>

                    <section class="seo-card">
                        <h2>SEO 점수</h2>
                        <div class="score"><strong>72</strong><span>/100</span></div>
                        <p>좋음</p>
                    </section>

                    <section class="metric-card">
                        <h2>성과 데이터</h2>
                        <div><span>조회수</span><strong><%= String.format("%,d", post.getViewCount()) %></strong></div>
                        <div><span>방문자</span><strong>1,234</strong></div>
                    </section>
                </aside>
            </section>
        </main>
    </div>
</body>
</html>
