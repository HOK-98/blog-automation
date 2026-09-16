<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>대시보드</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/home-dashboard.css">
</head>
<body>
    <div class="dashboard-shell">
        <jsp:include page="/WEB-INF/views/common/sidebar.jsp" />

        <main class="main-panel">
            <header class="hero-row">
                <div>
                    <h1>대시보드</h1>
                    <p>${activeSite.name} 운영 현황을 한눈에 확인하세요.</p>
                </div>
                <form method="post" action="${pageContext.request.contextPath}/posts/sync">
                    <input type="hidden" name="redirect" value="/dashboard">
                    <button type="submit">워드프레스 새로고침</button>
                </form>
            </header>

            <section class="metric-grid">
                <article><span>전체 글 수</span><strong>${statistics.totalPosts}</strong><em>DB 기준</em></article>
                <article><span>발행 완료</span><strong>${statistics.publishedPosts}</strong><em>DB 기준</em></article>
                <article><span>임시 저장</span><strong>${statistics.draftPosts}</strong><em>DB 기준</em></article>
                <article><span>최근 30일 방문자</span><strong><fmt:formatNumber value="${statistics.ga4Metrics.users}"/></strong><em>GA4 기준</em></article>
            </section>

            <section class="middle-grid">
                <article class="chart-card">
                    <div class="card-head"><h2>일자별 발행 추이</h2><button>게시글 수</button></div>
                    <div class="simple-bars">
                        <c:forEach var="row" items="${statistics.dailyPublishedCounts}">
                            <div>
                                <b>${row.count}</b>
                                <span style="height:${statistics.maxDailyPublishedCount == 0 ? 6 : row.count * 180 / statistics.maxDailyPublishedCount}px"></span>
                                <em>${row.day}</em>
                            </div>
                        </c:forEach>
                    </div>
                </article>

                <article class="progress-card">
                    <h2>운영 상태</h2>
                    <div class="progress-wrap">
                        <div class="ring" style="--progress:${statistics.totalPosts == 0 ? 0 : statistics.publishedPosts * 100 / statistics.totalPosts}%">
                            <strong>${statistics.totalPosts == 0 ? 0 : statistics.publishedPosts * 100 / statistics.totalPosts}%</strong>
                            <span>발행 비율</span>
                        </div>
                        <ul>
                            <li><span>전체 글</span><em>${statistics.totalPosts}</em><b>합계</b></li>
                            <li><span>발행 완료</span><em>${statistics.publishedPosts}</em><b>완료</b></li>
                            <li><span>임시 저장</span><em>${statistics.draftPosts}</em><b>대기</b></li>
                            <li><span>조회수</span><em><fmt:formatNumber value="${statistics.ga4Metrics.views}"/></em><b>GA4</b></li>
                            <li><span>평균 체류</span><em><fmt:formatNumber value="${statistics.ga4Metrics.averageSessionDuration}" minFractionDigits="2" maxFractionDigits="2"/></em><b>초</b></li>
                        </ul>
                    </div>
                </article>
            </section>

            <section class="lower-grid">
                <article class="recent-card">
                    <h2>최근 글</h2>
                    <ul>
                        <c:forEach var="post" items="${dashboard.posts}">
                            <li>
                                <b>W</b>
                                <span><a href="${pageContext.request.contextPath}/posts/detail?id=${post.id}">${post.title}</a><small>${post.category}</small></span>
                                <em>${post.status}</em>
                            </li>
                        </c:forEach>
                    </ul>
                </article>

                <article class="ranking-card">
                    <div class="card-head"><h2>인기 글 TOP 10</h2><button>조회수순</button></div>
                    <ol>
                        <c:forEach var="post" items="${dashboard.popularPosts}" varStatus="status">
                            <li><b>${status.count}</b><span><a href="${pageContext.request.contextPath}/posts/detail?id=${post.id}">${post.title}</a></span><em><fmt:formatNumber value="${post.viewCount}"/></em></li>
                        </c:forEach>
                    </ol>
                </article>
            </section>

            <section class="quick-actions">
                <a href="${pageContext.request.contextPath}/keywords/manage">키워드 관리</a>
                <a href="${pageContext.request.contextPath}/generation">본문 자동 생성</a>
                <a href="${pageContext.request.contextPath}/posts">블로그 글 목록</a>
                <a href="${pageContext.request.contextPath}/statistics">통계 보기</a>
            </section>
        </main>
    </div>
</body>
</html>
