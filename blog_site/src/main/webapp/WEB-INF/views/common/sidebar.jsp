<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<aside class="sidebar">
    <div class="brand">
        <span>⬢</span>
        <div>
            <strong>AutoBlog</strong>
            <small>AI 기반 블로그 자동 시스템</small>
        </div>
    </div>
    <nav class="side-nav">
        <a class="${activeMenu eq 'dashboard' ? 'active' : ''}" href="${pageContext.request.contextPath}/dashboard"><i>⌂</i>대시보드</a>
        <a class="${activeMenu eq 'posts' ? 'active' : ''}" href="${pageContext.request.contextPath}/posts"><i>▤</i>블로그 목록</a>
        <a class="${activeMenu eq 'keywords' ? 'active' : ''}" href="${pageContext.request.contextPath}/keywords/manage"><i>⌕</i>키워드</a>
        <a class="${activeMenu eq 'generation' ? 'active' : ''}" href="${pageContext.request.contextPath}/generation"><i>⚙</i>자동 생성 시스템</a>
        <a class="${activeMenu eq 'generation' ? 'active' : ''}" href="${pageContext.request.contextPath}/drafts"><i>✎</i>생성 글 검토</a>
        <a class="${activeMenu eq 'statistics' ? 'active' : ''}" href="${pageContext.request.contextPath}/statistics"><i>▥</i>통계</a>
        <a class="${activeMenu eq 'settings' ? 'active' : ''}" href="${pageContext.request.contextPath}/settings"><i>◌</i>설정</a>
    </nav>
    <div class="profile">
        <span class="avatar"></span>
        <div>
            <strong>관리자</strong>
            <small>admin@example.com</small>
        </div>
    </div>
    <form class="site-switcher" method="post" action="${pageContext.request.contextPath}/sites/switch">
        <select name="siteId" onchange="this.form.submit()">
            <c:forEach var="site" items="${sites}">
                <option value="${site.id}" ${site.active ? 'selected' : ''}>${site.name}</option>
            </c:forEach>
        </select>
    </form>
</aside>
