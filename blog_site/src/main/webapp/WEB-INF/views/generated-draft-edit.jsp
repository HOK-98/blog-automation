<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title>생성 글 검토</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/post-edit.css">
</head>
<body>
<div class="edit-shell">
    <jsp:include page="/WEB-INF/views/common/sidebar.jsp" />
    <main class="main-panel">
        <header class="topbar"><div><a href="${pageContext.request.contextPath}/drafts">← 목록으로</a><h1>생성 글 검토</h1></div></header>
        <form id="draftForm" method="post" action="${pageContext.request.contextPath}/drafts/update">
            <input type="hidden" name="id" value="${draft.id}">
            <section class="editor-grid">
                <article class="editor-card">
                    <label><span>제목</span><input type="text" name="title" value="${draft.title}"></label>
                    <div class="two-col-fields">
                        <label><span>슬러그</span><input type="text" name="slug" value="${draft.slug}"></label>
                        <label><span>워드프레스 카테고리</span>
                            <select name="wordpressCategoryId" id="wordpressCategorySelect">
                                <option value="">카테고리 선택 안 함</option>
                                <c:forEach var="category" items="${wordpressCategories}">
                                    <option value="${category.id}" data-name="${category.name}" ${draft.wordpressCategoryId eq category.id ? 'selected' : ''}>
                                        ${category.name}
                                    </option>
                                </c:forEach>
                            </select>
                        </label>
                        <c:if test="${not empty categoryLoadError}">
                            <p class="field-help">${categoryLoadError}</p>
                        </c:if>
                        <input type="hidden" name="categories" id="categoryNameField" value="${draft.categories}">
                    </div>
                    <label><span>요약</span><textarea name="excerpt" class="excerpt-editor">${draft.excerpt}</textarea></label>

                    <label><span>본문</span></label>
                    <div class="editor-toolbar">
                        <button type="button" data-command="bold"><b>B</b></button>
                        <button type="button" data-command="italic"><i>I</i></button>
                        <button type="button" data-command="underline"><u>U</u></button>
                        <button type="button" data-command="formatBlock" data-value="H2">H2</button>
                        <button type="button" data-command="formatBlock" data-value="H3">H3</button>
                        <button type="button" data-command="insertUnorderedList">• 목록</button>
                        <button type="button" data-command="insertOrderedList">1. 목록</button>
                        <button type="button" id="linkButton">링크</button>
                        <button type="button" data-command="removeFormat">서식 제거</button>
                    </div>
                    <div id="editor" class="rich-editor" contenteditable="true">${draft.contentHtml}</div>
                    <textarea id="contentField" name="contentHtml" class="hidden-content">${draft.contentHtml}</textarea>
                </article>
                <aside class="side-panel">
                    <section class="publish-card">
                        <h2>검토 상태</h2>
                        <label><span>상태</span>
                            <select name="status">
                                <option value="review" ${draft.status eq 'review' ? 'selected' : ''}>검토중</option>
                                <option value="hold" ${draft.status eq 'hold' ? 'selected' : ''}>보류</option>
                                <option value="published" ${draft.status eq 'published' ? 'selected' : ''}>발행완료</option>
                            </select>
                        </label>
                        <div><span>검증 점수</span><strong>${draft.verificationScore}</strong></div>
                        <div><span>토큰 사용량</span><strong>${draft.totalTokens}</strong></div>
                        <div><span>입력/출력</span><strong>${draft.promptTokens} / ${draft.completionTokens}</strong></div>
                        <button type="submit">저장</button>
                    </section>
                    <section class="help-card draft-actions">
                        <p>화면에서는 일반 글처럼 편집하고, 저장/발행 시에는 HTML 본문으로 전송됩니다.</p>
                        <button type="submit" formaction="${pageContext.request.contextPath}/drafts/publish" formmethod="post">워드프레스에 발행</button>
                        <button type="submit" formaction="${pageContext.request.contextPath}/drafts/delete" formmethod="post">삭제</button>
                    </section>
                </aside>
            </section>
        </form>
    </main>
</div>
<script src="${pageContext.request.contextPath}/assets/js/post-edit.js"></script>
</body>
</html>
