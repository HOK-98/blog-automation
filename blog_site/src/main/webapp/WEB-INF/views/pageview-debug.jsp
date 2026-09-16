<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>페이지 조회수 매칭 점검</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/statistics.css">
</head>
<body>
<div class="statistics-shell">
    <jsp:include page="/WEB-INF/views/common/sidebar.jsp" />
    <main class="main-panel">
        <header class="topbar">
            <div>
                <h1>페이지 조회수 매칭 점검</h1>
                <p>워드프레스 글 URL과 GA4 pagePath 매칭 결과를 확인합니다.</p>
            </div>
        </header>
        <section class="debug-card">
            <table>
                <thead>
                <tr>
                    <th>제목</th>
                    <th>워드프레스 경로</th>
                    <th>GA4 경로</th>
                    <th>GA4 조회수</th>
                    <th>매칭</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="row" items="${matches}">
                    <tr>
                        <td>${row.title}</td>
                        <td>${row.wordpressPath}</td>
                        <td>${row.matchedGa4Path}</td>
                        <td>${row.ga4Views}</td>
                        <td><c:choose><c:when test="${row.matched}">일치</c:when><c:otherwise>불일치</c:otherwise></c:choose></td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </section>
    </main>
</div>
</body>
</html>
