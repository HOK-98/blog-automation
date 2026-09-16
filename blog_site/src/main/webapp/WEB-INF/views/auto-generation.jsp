<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>자동 생성 시스템</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/generation-progress.css">
</head>
<body>
    <div class="progress-shell">
        <jsp:include page="/WEB-INF/views/common/sidebar.jsp" />

        <main class="main-panel">
            <header class="topbar">
                <div>
                    <h1>자동 생성 시스템</h1>
                    <p>키워드를 입력하면 기존 파이썬 자동화 엔진으로 글 초안을 생성합니다.</p>
                </div>
            </header>

            <section class="content-grid">
                <c:if test="${not empty error}">
                    <p class="notice error">${error}</p>
                </c:if>
                <c:if test="${autoPublished}">
                    <p class="notice">자동 발행 설정에 따라 워드프레스에 바로 발행했습니다.</p>
                </c:if>
                <article class="setting-card">
                    <h2>생성 설정</h2>
                    <form method="get" action="${pageContext.request.contextPath}/generation">
                        <input type="hidden" name="action" value="generate">
                        <label><span>키워드</span><input type="text" name="keyword" value="${keyword}" placeholder="예: AI 글쓰기 도구 추천"></label>
                        <button class="primary-button" type="submit">미리보기 생성</button>
                    </form>
                </article>

                <article class="status-card">
                    <h2>AI 생성 진행 상황</h2>
                    <c:choose>
                        <c:when test="${empty preview}">
                            <div class="ring"><strong>0%</strong></div>
                            <h3>대기 중</h3>
                            <p>키워드를 입력하면 생성이 시작됩니다.</p>
                        </c:when>
                        <c:otherwise>
                            <div class="ring complete"><strong>${preview.score}</strong></div>
                            <h3>${preview.approved ? '검증 통과' : '검토 필요'}</h3>
                            <p>2차 검증 점수 기준으로 초안을 평가했습니다.</p>
                            <p>토큰 사용량: 입력 ${preview.promptTokens} / 출력 ${preview.completionTokens} / 합계 ${preview.totalTokens}</p>
                            <ul>
                                <li>키워드 분석 완료</li>
                                <li>본문 생성 완료</li>
                                <li>검증 완료</li>
                                <c:forEach var="issue" items="${preview.issues}">
                                    <li class="issue">${issue}</li>
                                </c:forEach>
                            </ul>
                        </c:otherwise>
                    </c:choose>
                </article>

                <article class="preview-card">
                    <h2>생성 결과 미리보기</h2>
                    <c:choose>
                        <c:when test="${empty preview}">
                            <p class="empty-copy">아직 생성된 초안이 없습니다.</p>
                        </c:when>
                        <c:otherwise>
                            <label><span>제목</span><input type="text" value="${preview.title}" readonly></label>
                            <label><span>슬러그</span><input type="text" value="${preview.slug}" readonly></label>
                            <div class="outline">
                                <h3>${preview.keyword}</h3>
                                <p>${preview.excerpt}</p>
                            </div>
                            <small>카테고리: <c:forEach var="category" items="${preview.categories}">${category} </c:forEach></small>
                            <small>태그: <c:forEach var="tag" items="${preview.tags}">#${tag} </c:forEach></small>
                        </c:otherwise>
                    </c:choose>
                </article>
            </section>
        </main>
    </div>
</body>
</html>
