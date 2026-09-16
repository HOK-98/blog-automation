<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>블로그 목록</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/dashboard.css">
</head>
<body>
    <div class="list-shell">
        <jsp:include page="/WEB-INF/views/common/sidebar.jsp" />

        <main class="main-panel">
            <header class="topbar">
                <div>
                    <h1>블로그 목록</h1>
                    <p>발행된 블로그 글을 관리하고 확인하세요.</p>
                </div>
                <form method="post" action="${pageContext.request.contextPath}/posts/sync">
                    <input type="hidden" name="redirect" value="/posts">
                    <button type="submit">워드프레스 새로고침</button>
                </form>
            </header>

            <form class="filters" method="get" action="${pageContext.request.contextPath}/posts">
                <input type="text" name="q" value="${q}" placeholder="제목, 키워드 검색...">
                <select name="status">
                    <option value="all">전체 상태</option>
                    <c:forEach var="status" items="${dashboard.statusCounts.keySet()}">
                        <option value="${status}" ${status eq selectedStatus ? 'selected' : ''}>${status}</option>
                    </c:forEach>
                </select>
                <select name="category">
                    <option value="all">전체 카테고리</option>
                    <c:forEach var="category" items="${dashboard.categoryCounts.keySet()}">
                        <option value="${category}" ${category eq selectedCategory ? 'selected' : ''}>${category}</option>
                    </c:forEach>
                </select>
                <select><option>전체 기간</option></select>
                <button type="button">+ 새 글 작성</button>
            </form>

            <section class="summary-grid">
                <article><span>전체 글</span><strong>${dashboard.totalCount}</strong></article>
                <article><span>발행 완료</span><strong>${empty dashboard.statusCounts['발행됨'] ? 0 : dashboard.statusCounts['발행됨']}</strong></article>
                <article><span>예약 발행</span><strong>${empty dashboard.statusCounts['예약됨'] ? 0 : dashboard.statusCounts['예약됨']}</strong></article>
                <article><span>임시 저장</span><strong>${empty dashboard.statusCounts['임시저장'] ? 0 : dashboard.statusCounts['임시저장']}</strong></article>
                <article><span>휴지통</span><strong>0</strong></article>
            </section>

            <section class="table-card">
                <table>
                    <thead>
                        <tr><th></th><th>제목</th><th>카테고리</th><th>상태</th><th>발행일</th><th>조회수</th><th>작업</th></tr>
                    </thead>
                    <tbody>
                        <c:forEach var="post" items="${dashboard.posts}">
                        <tr>
                            <td><input type="checkbox"></td>
                            <td><a href="${pageContext.request.contextPath}/posts/detail?id=${post.id}">${post.title}</a></td>
                            <td>${post.category}</td>
                            <td><span class="badge ${post.status eq '발행됨' ? 'green' : 'orange'}">${post.status}</span></td>
                            <td>${post.publishedAt}</td>
                            <td><fmt:formatNumber value="${post.viewCount}" pattern="#,###"/></td>
                            <td class="actions">⌕　◔　⋮</td>
                        </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </section>

            <footer class="list-footer">
                <div class="pagination">
                    <a href="#">‹</a>
                    <c:forEach var="i" begin="1" end="${dashboard.totalPages}">
                        <a class="${i eq dashboard.currentPage ? 'active' : ''}" href="?q=${q}&category=${selectedCategory}&status=${selectedStatus}&page=${i}">${i}</a>
                    </c:forEach>
                    <a href="#">›</a>
                </div>
                <span>${dashboard.currentPage}페이지 / ${dashboard.totalPages}페이지</span>
            </footer>
        </main>
    </div>
</body>
</html>
