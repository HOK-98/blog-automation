<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title>키워드 관리</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/keyword-management.css">
</head>
<body>
<div class="keyword-shell">
    <jsp:include page="/WEB-INF/views/common/sidebar.jsp" />
    <main class="main-panel">
        <header class="topbar">
            <div><h1>키워드 관리</h1><p>카테고리를 선택해 그 주제에 맞는 키워드를 수집합니다.</p></div>
        </header>
        <section class="metric-grid">
            <article><span>전체 키워드</span><strong>${keywordCount}</strong></article>
        </section>
        <form class="filters collect-form" method="get" action="${pageContext.request.contextPath}/keywords/manage">
            <input type="hidden" name="action" value="collect">
            <select name="category">
                <c:forEach var="category" items="${dashboard.categoryCounts.keySet()}">
                    <option value="${category}">${category}</option>
                </c:forEach>
            </select>
            <select name="count">
                <option value="5">5개</option>
                <option value="10">10개</option>
                <option value="15">15개</option>
            </select>
            <button type="submit">선택 카테고리로 키워드 수집</button>
        </form>
        <c:if test="${not empty added}">
            <p class="notice">${added}개의 새 키워드를 추가했습니다.</p>
        </c:if>
        <c:if test="${disabled eq 'keyword'}">
            <p class="notice error">설정에서 자동 키워드 수집이 꺼져 있습니다.</p>
        </c:if>
        <form class="filters" method="get" action="${pageContext.request.contextPath}/keywords/manage">
            <input type="text" name="q" value="${q}" placeholder="키워드 검색...">
            <select name="categoryFilter">
                <option value="all">전체 카테고리</option>
                <c:forEach var="category" items="${dashboard.categoryCounts.keySet()}">
                    <option value="${category}" ${category eq selectedCategory ? 'selected' : ''}>${category}</option>
                </c:forEach>
            </select>
            <button type="submit">검색</button>
        </form>
        <form method="post" action="${pageContext.request.contextPath}/keywords/delete">
        <section class="table-card">
            <table>
                <thead><tr><th><input type="checkbox" id="checkAll"></th><th>키워드</th><th>카테고리</th><th>수요 점수</th><th>상태</th><th>수집일</th><th>작업</th></tr></thead>
                <tbody>
                <c:forEach var="item" items="${keywords}">
                    <c:url var="generationUrl" value="/generation">
                        <c:param name="keyword" value="${item.keyword}" />
                    </c:url>
                    <c:url var="analysisUrl" value="/keywords">
                        <c:param name="keyword" value="${item.keyword}" />
                    </c:url>
                    <tr>
                        <td><input type="checkbox" name="ids" value="${item.id}"></td>
                        <td><a href="${generationUrl}">${item.keyword}</a></td>
                        <td>${item.groupName}</td>
                        <td>
                            <span class="score-badge muted">분석 필요</span>
                            <small class="trend-label">클릭 시 조회</small>
                        </td>
                        <td>${item.status}</td>
                        <td>${item.createdAt}</td>
                        <td><a class="analysis-link" href="${analysisUrl}">분석</a></td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </section>
        <div class="table-actions">
            <button type="submit">선택 삭제</button>
        </div>
        </form>
    </main>
</div>
<script>
document.getElementById("checkAll").addEventListener("change", function () {
    var checked = this.checked;
    document.querySelectorAll('input[name="ids"]').forEach(function (box) {
        box.checked = checked;
    });
});
</script>
</body>
</html>
