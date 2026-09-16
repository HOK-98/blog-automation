<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title>키워드 분석</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/keyword-analysis.css">
</head>
<body>
<div class="keyword-shell">
    <jsp:include page="/WEB-INF/views/common/sidebar.jsp" />
    <main class="keyword-main">
        <section class="page-head"><h1>키워드 분석</h1><p>Naver DataLab 기반 상대 검색 추세를 확인합니다.</p></section>
        <form class="search-row" method="get" action="${pageContext.request.contextPath}/keywords">
            <input type="text" name="keyword" value="${keyword}" placeholder="키워드를 입력하세요">
            <button type="submit">분석하기</button>
        </form>
        <section class="metric-grid">
            <article><span>현재 키워드</span><strong>${keyword}</strong></article>
            <article>
                <span>수요 점수</span>
                <strong>${empty trend ? 0 : trend.demandScore}</strong>
                <small>${empty trend ? '데이터 없음' : trend.demandLabel}</small>
            </article>
            <article>
                <span>최근 7일 평균 관심도</span>
                <strong><fmt:formatNumber value="${empty trend ? 0 : trend.recentAverageRatio}" maxFractionDigits="1"/></strong>
                <small>${empty trend ? '' : trend.trendLabel}</small>
            </article>
            <article>
                <span>전체 평균 관심도</span>
                <strong><fmt:formatNumber value="${empty trend ? 0 : trend.averageRatio}" maxFractionDigits="1"/></strong>
                <small>${empty trend ? '-' : trend.startDate} ~ ${empty trend ? '-' : trend.endDate}</small>
            </article>
        </section>
        <section class="dashboard-grid">
            <article class="trend-card">
                <div class="card-title">
                    <h2>검색 추이</h2>
                    <c:if test="${not empty trend}">
                        <b class="demand-pill">${trend.trendLabel}</b>
                    </c:if>
                </div>
                <c:choose>
                    <c:when test="${empty trend or empty trend.points}">
                        <p class="empty-copy">분석할 키워드를 입력하면 네이버 데이터랩 기반 검색 추이가 표시됩니다.</p>
                    </c:when>
                    <c:otherwise>
                        <div class="trend-bars">
                            <c:forEach var="point" items="${trend.points}">
                                <div>
                                    <b><fmt:formatNumber value="${point.ratio}" maxFractionDigits="0"/></b>
                                    <span style="height:${point.ratio * 1.6}px"></span>
                                    <em>${point.period}</em>
                                </div>
                            </c:forEach>
                        </div>
                    </c:otherwise>
                </c:choose>
            </article>
            <article class="rank-card">
                <h2>분석 해석</h2>
                <c:if test="${not empty trend}">
                    <div class="insight-box">
                        <strong>${trend.demandLabel}</strong>
                        <p>
                            수요 점수는 최근 30일 상대 검색 관심도와 최근 7일 흐름을 합산한 값입니다.
                            실제 월 검색량은 아니지만, 글 작성 우선순위 판단에 사용할 수 있습니다.
                        </p>
                    </div>
                </c:if>
                <h2>관련 키워드</h2>
                <ol>
                    <c:forEach var="item" items="${relatedKeywords}">
                        <li><span>${item.keyword}</span><em>${item.groupName}</em></li>
                    </c:forEach>
                </ol>
            </article>
        </section>
    </main>
</div>
</body>
</html>
