<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>설정</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/settings.css">
</head>
<body>
    <div class="settings-shell">
        <jsp:include page="/WEB-INF/views/common/sidebar.jsp" />

        <main class="main-panel">
            <header class="page-head">
                <div>
                    <h1>설정</h1>
                    <p>사이트 연결과 자동화 동작을 관리합니다.</p>
                </div>
                <div class="page-actions">
                    <a class="ghost-link" href="${pageContext.request.contextPath}/settings?new=1">+ 새 사이트 추가</a>
                    <form class="top-site-switcher" method="post" action="${pageContext.request.contextPath}/sites/switch">
                    <label>
                        <span>현재 사이트</span>
                        <select name="siteId" onchange="this.form.submit()">
                            <c:forEach var="site" items="${sites}">
                                <option value="${site.id}" ${site.active ? 'selected' : ''}>${site.name}</option>
                            </c:forEach>
                        </select>
                    </label>
                    </form>
                </div>
            </header>

            <nav class="tabs" id="settingsTabs">
                <a class="active" data-tab="general">일반 설정</a>
                <a data-tab="wordpress">워드프레스 설정</a>
                <a data-tab="ai">AI 설정</a>
                <a data-tab="seo">SEO 설정</a>
                <a data-tab="notification">알림 설정</a>
                <a data-tab="sites">사이트 관리</a>
            </nav>
            <c:if test="${param.test eq 'ok'}"><p class="settings-notice">워드프레스 연결 테스트에 성공했습니다.</p></c:if>
            <c:if test="${param.test eq 'fail'}"><p class="settings-notice error">워드프레스 연결 테스트에 실패했습니다. URL, 사용자명, Application Password를 확인하세요.</p></c:if>
            <c:if test="${param.runNow eq 'ok' or param.runNow eq 'started'}"><p class="settings-notice">자동화를 백그라운드에서 시작했습니다. 생성 결과는 생성 글 검토/블로그 목록에서 확인하세요.</p></c:if>
            <c:if test="${param.runNow eq 'running'}"><p class="settings-notice">이미 자동화가 실행 중입니다. 현재 작업이 끝난 뒤 다시 실행할 수 있습니다.</p></c:if>
            <c:if test="${param.runNow eq 'fail'}"><p class="settings-notice error">자동화 즉시 실행에 실패했습니다. 서버 로그를 확인하세요.</p></c:if>

            <form class="settings-grid" method="post" action="${pageContext.request.contextPath}/sites/save">
                <input type="hidden" name="id" value="${activeSite.id}">

                <article class="general-card" data-panel="general">
                    <h2><c:choose><c:when test="${creatingNewSite}">새 사이트 추가</c:when><c:otherwise>사이트 정보</c:otherwise></c:choose></h2>
                    <label><span>사이트 이름</span><input type="text" name="name" value="${activeSite.name}" required></label>
                    <label><span>사이트 설명</span><textarea name="description">${activeSite.description}</textarea></label>
                    <label data-panel="wordpress"><span>사이트 URL</span><input id="siteUrlInput" type="text" name="siteUrl" value="${activeSite.siteUrl}" required></label>
                    <label data-panel="wordpress"><span>Posts API URL</span><input id="postsApiUrlInput" type="text" name="postsApiUrl" value="${activeSite.postsApiUrl}" required></label>
                    <label><span>관리자 이메일</span><input type="email" name="adminEmail" value="${activeSite.adminEmail}"></label>
                    <label data-panel="wordpress"><span>워드프레스 사용자명</span><input type="text" name="username" value="${activeSite.username}" required></label>
                    <label data-panel="wordpress"><span>Application Password</span><input type="password" name="applicationPassword" value="${activeSite.applicationPassword}" required></label>
                    <div data-panel="wordpress" class="inline-actions">
                        <a href="${pageContext.request.contextPath}/sites/test">워드프레스 연결 테스트</a>
                    </div>
                    <div class="two-col">
                        <label><span>시간대</span><input type="text" name="timezone" value="${activeSite.timezone}"></label>
                        <label><span>언어</span><input type="text" name="language" value="${activeSite.language}"></label>
                    </div>
                    <label data-panel="seo"><span>기본 발행 상태</span>
                        <select name="defaultPostStatus">
                            <option value="draft" ${activeSite.defaultPostStatus eq 'draft' ? 'selected' : ''}>임시 저장</option>
                            <option value="publish" ${activeSite.defaultPostStatus eq 'publish' ? 'selected' : ''}>즉시 발행</option>
                            <option value="future" ${activeSite.defaultPostStatus eq 'future' ? 'selected' : ''}>예약 발행</option>
                            <option value="pending" ${activeSite.defaultPostStatus eq 'pending' ? 'selected' : ''}>검토 대기</option>
                        </select>
                    </label>
                    <label data-panel="seo"><span>SEO 제목 접미사</span><input type="text" name="seoTitleSuffix" value="${activeSite.seoTitleSuffix}" placeholder="예: | Growcha"></label>
                    <label data-panel="seo"><span>메타 설명 템플릿</span><textarea name="metaDescriptionTemplate" placeholder="{excerpt}, {title}, {keyword} 사용 가능">${activeSite.metaDescriptionTemplate}</textarea></label>
                    <button type="submit">저장하기</button>
                </article>

                <aside>
                    <section class="system-card" data-panel="ai">
                        <h2>자동화 설정</h2>
                        <label class="toggle-row"><span>자동 키워드 수집</span><input type="checkbox" name="autoKeywordCollection" ${activeSite.autoKeywordCollection ? 'checked' : ''}></label>
                        <label class="toggle-row"><span>자동 본문 생성</span><input type="checkbox" name="autoContentGeneration" ${activeSite.autoContentGeneration ? 'checked' : ''}></label>
                        <label class="toggle-row"><span>자동 발행</span><input type="checkbox" name="autoPublishing" ${activeSite.autoPublishing ? 'checked' : ''}></label>
                        <label class="toggle-row"><span>예약 자동 실행</span><input type="checkbox" name="scheduleEnabled" ${activeSite.scheduleEnabled ? 'checked' : ''}></label>
                        <label><span>자동 시작 시간</span><input type="time" name="scheduleStartTime" value="${empty activeSite.scheduleStartTime ? '09:00' : activeSite.scheduleStartTime}"></label>
                        <div class="two-col">
                            <label><span>키워드 수집 개수</span><input type="number" name="scheduledKeywordCount" min="1" max="20" value="${activeSite.scheduledKeywordCount == 0 ? 5 : activeSite.scheduledKeywordCount}"></label>
                            <label><span>글 작성 개수</span><input type="number" name="scheduledPostCount" min="1" max="10" value="${activeSite.scheduledPostCount == 0 ? 1 : activeSite.scheduledPostCount}"></label>
                        </div>
                        <label><span>수집 카테고리</span>
                            <select name="scheduleCategory">
                                <option value="">자동 선택</option>
                                <c:forEach var="category" items="${wordpressCategories}">
                                    <option value="${category.name}" ${category.name eq activeSite.scheduleCategory ? 'selected' : ''}>${category.name}</option>
                                </c:forEach>
                            </select>
                        </label>
                        <c:if test="${not empty categoryLoadError}">
                            <p class="settings-hint">${categoryLoadError}</p>
                        </c:if>
                        <p class="settings-hint">선택한 워드프레스 카테고리에 맞는 키워드를 수집하고, 수집된 키워드로 바로 글을 생성합니다. 자동 선택이면 워드프레스 카테고리 중 하나를 사용합니다.</p>
                        <p class="settings-hint">서버가 켜져 있어야 실행됩니다. 같은 날짜에는 한 번만 실행됩니다.</p>
                        <div class="inline-actions">
                            <a href="${pageContext.request.contextPath}/automation/run-now">지금 바로 실행</a>
                        </div>
                        <label class="toggle-row" data-panel="notification"><span>알림 전송</span><input type="checkbox" name="notificationEnabled" ${activeSite.notificationEnabled ? 'checked' : ''}></label>
                        <label data-panel="notification"><span>알림 받을 이메일</span><input type="email" name="notificationEmail" value="${activeSite.notificationEmail}" placeholder="admin@example.com"></label>
                        <label class="toggle-row" data-panel="notification"><span>발행 완료 시 알림</span><input type="checkbox" name="notifyOnPublish" ${activeSite.notifyOnPublish ? 'checked' : ''}></label>
                        <label class="toggle-row"><span>이미지 자동 생성</span><input type="checkbox" name="autoImageGeneration" ${activeSite.autoImageGeneration ? 'checked' : ''}></label>
                        <label class="toggle-row" data-panel="seo"><span>내부 링크 자동 추가</span><input type="checkbox" name="autoInternalLinks" ${activeSite.autoInternalLinks ? 'checked' : ''}></label>
                    </section>

                    <section class="info-card" data-panel="sites">
                        <h2>등록된 사이트</h2>
                        <ul class="site-list">
                            <c:forEach var="site" items="${sites}">
                                <li class="${site.active ? 'active' : ''}">
                                    <strong>${site.name}</strong>
                                    <small>${site.siteUrl}</small>
                                </li>
                            </c:forEach>
                        </ul>
                    </section>
                </aside>
            </form>
        </main>
    </div>
<script>
(function () {
    var tabs = document.querySelectorAll("#settingsTabs [data-tab]");
    var panels = document.querySelectorAll("[data-panel]");
    function show(tab) {
        tabs.forEach(function (item) {
            item.classList.toggle("active", item.getAttribute("data-tab") === tab);
        });
        panels.forEach(function (panel) {
            var group = panel.getAttribute("data-panel");
            panel.style.display = (tab === "general" || group === tab || group === "general") ? "" : "none";
        });
    }
    tabs.forEach(function (tab) {
        tab.addEventListener("click", function () {
            show(tab.getAttribute("data-tab"));
        });
    });
    show("general");

    var siteUrl = document.getElementById("siteUrlInput");
    var postsApiUrl = document.getElementById("postsApiUrlInput");
    if (siteUrl && postsApiUrl) {
        siteUrl.addEventListener("blur", function () {
            if (!postsApiUrl.value && siteUrl.value) {
                postsApiUrl.value = siteUrl.value.replace(/\/+$/, "") + "/wp-json/wp/v2/posts";
            }
        });
    }
})();
</script>
</body>
</html>
