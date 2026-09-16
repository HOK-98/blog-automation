<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>통계</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/statistics.css">
</head>
<body>
    <div class="statistics-shell">
        <jsp:include page="/WEB-INF/views/common/sidebar.jsp" />

        <main class="main-panel">
            <header class="topbar">
                <div>
                    <h1>통계</h1>
                    <p>블로그 성과와 데이터 인사이트를 한눈에 확인하세요.</p>
                </div>
                <button>현재 데이터 기준　▣</button>
            </header>

            <nav class="tabs">
                <button class="active" data-tab="overview">개요</button>
                <button data-tab="content">콘텐츠</button>
                <button data-tab="traffic">트래픽</button>
                <button data-tab="seo">SEO</button>
                <button data-tab="revenue">수익</button>
            </nav>

            <section class="metric-grid tab-panel active" data-panel="overview">
                <article><span>총 조회수</span><strong>${statistics.ga4Metrics.views}</strong><em>GA4</em></article>
                <article><span>총 방문자</span><strong>${statistics.ga4Metrics.users}</strong><em>GA4</em></article>
                <article><span>평균 체류 시간</span><strong><fmt:formatNumber value="${statistics.ga4Metrics.averageSessionDuration}" minFractionDigits="2" maxFractionDigits="2"/></strong><em>GA4</em></article>
                <article><span>이탈률</span><strong><fmt:formatNumber value="${statistics.ga4Metrics.bounceRate}" minFractionDigits="2" maxFractionDigits="2"/></strong><em>GA4</em></article>
                <article><span>전체 글 수</span><strong>${statistics.totalPosts}</strong><em>DB</em></article>
            </section>

            <section class="chart-grid tab-panel active" data-panel="overview">
                <article class="line-card">
                    <h2>일자별 발행 추이</h2>
                    <div class="simple-bars">
                        <c:forEach var="row" items="${statistics.dailyPublishedCounts}">
                            <div>
                                <b>${row.count}</b>
                                <span style="height:${row.count * 18}px"></span>
                                <em>${row.day}</em>
                            </div>
                        </c:forEach>
                    </div>
                </article>

                <article class="source-card">
                    <h2>카테고리별 글 수</h2>
                    <ul class="category-list">
                        <c:forEach var="channel" items="${statistics.ga4Metrics.channels}">
                            <li><span>${channel.name}</span><em>${channel.users}</em></li>
                        </c:forEach>
                    </ul>
                </article>
            </section>

            <section class="lower-grid tab-panel active" data-panel="overview">
                <article class="rank-card">
                    <h2>인기 글 TOP 5</h2>
                    <table>
                        <thead><tr><th>순위</th><th>제목</th><th>조회수</th></tr></thead>
                        <tbody>
                            <c:forEach var="post" items="${statistics.popularPosts}" varStatus="status">
                                <tr><td>${status.count}</td><td>${post.title}</td><td>${post.viewCount}</td></tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </article>

                <article class="device-card">
                    <h2>기기별 비율</h2>
                    <ul class="category-list">
                        <c:forEach var="device" items="${statistics.ga4Metrics.devices}">
                            <li><span>${device.name}</span><em>${device.users}</em></li>
                        </c:forEach>
                    </ul>
                </article>
            </section>

            <section class="content-panel tab-panel" data-panel="content">
                <article>
                    <h2>콘텐츠 현황</h2>
                    <div class="mini-grid">
                        <div><span>전체 글 수</span><strong>${statistics.totalPosts}</strong></div>
                        <div><span>발행 완료</span><strong>${statistics.publishedPosts}</strong></div>
                        <div><span>임시 저장</span><strong>${statistics.draftPosts}</strong></div>
                        <div><span>카테고리 수</span><strong>${statistics.categoryCounts.size()}</strong></div>
                    </div>
                </article>
                <article>
                    <h2>카테고리별 글 수</h2>
                    <ul class="category-list">
                        <c:forEach var="entry" items="${statistics.categoryCounts}">
                            <li><span>${entry.key}</span><em>${entry.value}</em></li>
                        </c:forEach>
                    </ul>
                </article>
            </section>

            <section class="content-panel tab-panel" data-panel="traffic">
                <article>
                    <h2>트래픽 개요</h2>
                    <div class="mini-grid">
                        <div><span>총 조회수</span><strong>${statistics.ga4Metrics.views}</strong></div>
                        <div><span>총 방문자</span><strong>${statistics.ga4Metrics.users}</strong></div>
                        <div><span>평균 체류 시간</span><strong><fmt:formatNumber value="${statistics.ga4Metrics.averageSessionDuration}" minFractionDigits="2" maxFractionDigits="2"/></strong></div>
                        <div><span>이탈률</span><strong><fmt:formatNumber value="${statistics.ga4Metrics.bounceRate}" minFractionDigits="2" maxFractionDigits="2"/></strong></div>
                    </div>
                </article>
                <article>
                    <h2>유입 채널</h2>
                    <ul class="category-list">
                        <c:forEach var="channel" items="${statistics.ga4Metrics.channels}">
                            <li><span>${channel.name}</span><em>${channel.users}</em></li>
                        </c:forEach>
                    </ul>
                </article>
                <article>
                    <h2>기기별 사용자</h2>
                    <ul class="category-list">
                        <c:forEach var="device" items="${statistics.ga4Metrics.devices}">
                            <li><span>${device.name}</span><em>${device.users}</em></li>
                        </c:forEach>
                    </ul>
                </article>
            </section>

            <section class="content-panel tab-panel" data-panel="seo">
                <article>
                    <h2>SEO 현황</h2>
                    <div class="mini-grid">
                        <div><span>평균 SEO 점수</span><strong>72</strong></div>
                        <div><span>최적화 완료 글</span><strong>${statistics.publishedPosts}</strong></div>
                        <div><span>개선 필요 글</span><strong>${statistics.draftPosts}</strong></div>
                        <div><span>등록 카테고리</span><strong>${statistics.categoryCounts.size()}</strong></div>
                    </div>
                </article>
                <article>
                    <h2>SEO 개선 항목</h2>
                    <ul class="check-list">
                        <li>메타 설명 점검</li>
                        <li>제목 길이 확인</li>
                        <li>내부 링크 추가</li>
                        <li>대표 이미지 alt 점검</li>
                    </ul>
                </article>
            </section>

            <section class="content-panel tab-panel" data-panel="revenue">
                <article>
                    <h2>수익 개요</h2>
                    <div class="mini-grid">
                        <div><span>예상 수익</span><strong>₩0</strong></div>
                        <div><span>RPM</span><strong>₩0</strong></div>
                        <div><span>광고 클릭</span><strong>0</strong></div>
                        <div><span>수익 연동</span><strong>대기</strong></div>
                    </div>
                </article>
                <article>
                    <h2>안내</h2>
                    <p class="muted-copy">수익 탭은 애드센스 또는 광고 플랫폼 연동 후 실제 값으로 활성화할 수 있습니다.</p>
                </article>
            </section>
        </main>
    </div>
<script src="${pageContext.request.contextPath}/assets/js/statistics.js"></script>
</body>
</html>
