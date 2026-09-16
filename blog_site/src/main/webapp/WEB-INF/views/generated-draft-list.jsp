<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title>생성 글 검토</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/dashboard.css">
</head>
<body>
<div class="list-shell">
    <jsp:include page="/WEB-INF/views/common/sidebar.jsp" />
    <main class="main-panel">
        <header class="topbar"><div><h1>생성 글 검토</h1><p>자동 생성된 초안을 보고 발행 여부를 결정합니다.</p></div></header>
        <form class="filters" method="get" action="${pageContext.request.contextPath}/drafts">
            <label class="inline-check"><input type="checkbox" name="all" value="1" ${allSites ? 'checked' : ''}> 전체 사이트 보기</label>
            <select name="siteId">
                <option value="">현재 사이트</option>
                <c:forEach var="site" items="${sites}">
                    <option value="${site.id}" ${site.id eq selectedSiteId ? 'selected' : ''}>${site.name}</option>
                </c:forEach>
            </select>
            <button type="submit">적용</button>
        </form>
        <form method="post" action="${pageContext.request.contextPath}/drafts/delete">
        <section class="table-card">
            <table>
                <thead><tr><th><input type="checkbox" id="checkAll"></th><th>사이트</th><th>제목</th><th>키워드</th><th>점수</th><th>토큰</th><th>상태</th><th>작업</th></tr></thead>
                <tbody>
                <c:forEach var="draft" items="${drafts}">
                    <tr>
                        <td><input type="checkbox" name="ids" value="${draft.id}"></td>
                        <td>${draft.siteName}</td>
                        <td><a href="${pageContext.request.contextPath}/drafts/edit?id=${draft.id}">${draft.title}</a></td>
                        <td>${draft.keyword}</td>
                        <td>${draft.verificationScore}</td>
                        <td>${draft.totalTokens}</td>
                        <td>${draft.status}</td>
                        <td><a href="${pageContext.request.contextPath}/drafts/edit?id=${draft.id}">열기</a></td>
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
var checkAll = document.getElementById("checkAll");
if (checkAll) {
    checkAll.addEventListener("change", function () {
        var checked = this.checked;
        document.querySelectorAll('input[name="ids"]').forEach(function (box) {
            box.checked = checked;
        });
    });
}
</script>
</body>
</html>
