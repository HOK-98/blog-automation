<%@ page import="com.autoblog.model.BlogPost"%>
<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<%
    BlogPost post = (BlogPost) request.getAttribute("post");
%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>글 수정</title>
<link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/post-edit.css">
</head>
<body>
    <div class="edit-shell">
        <jsp:include page="/WEB-INF/views/common/sidebar.jsp" />

        <main class="main-panel">
            <header class="topbar">
                <div>
                    <a href="<%= request.getContextPath() %>/posts/detail?id=<%= post.getId() %>">‹ 상세로 돌아가기</a>
                    <h1>글 수정</h1>
                </div>
            </header>

            <form method="post" action="<%= request.getContextPath() %>/posts/update">
                <input type="hidden" name="id" value="<%= post.getId() %>">

                <section class="editor-grid">
                    <article class="editor-card">
                        <label>
                            <span>제목</span>
                            <input type="text" name="title" value="<%= post.getTitle() %>">
                        </label>
                        <label>
                            <span>본문</span>
                        </label>
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
                        <div id="editor" class="rich-editor" contenteditable="true"><%= post.getContent() %></div>
                        <textarea id="contentField" name="content" class="hidden-content"><%= post.getContent() %></textarea>
                    </article>

                    <aside class="side-panel">
                        <section class="publish-card">
                            <h2>발행 설정</h2>
                            <label>
                                <span>상태</span>
                                <select name="status">
                                    <option value="publish" <%= "발행됨".equals(post.getStatus()) ? "selected" : "" %>>발행됨</option>
                                    <option value="draft" <%= "draft".equals(post.getStatus()) || "임시저장".equals(post.getStatus()) ? "selected" : "" %>>임시저장</option>
                                </select>
                            </label>
                            <div>
                                <span>워드프레스 ID</span>
                                <strong><%= post.getWordpressPostId() == null ? "-" : post.getWordpressPostId() %></strong>
                            </div>
                            <div>
                                <span>카테고리</span>
                                <strong><%= post.getCategory() %></strong>
                            </div>
                            <button type="submit">저장하기</button>
                        </section>

                        <section class="help-card">
                            <h2>편집 안내</h2>
                            <p>현재는 워드프레스 HTML을 직접 편집하는 방식입니다. 저장하면 워드프레스 원문과 관리자 화면이 함께 갱신됩니다.</p>
                        </section>
                    </aside>
                </section>
            </form>
        </main>
    </div>
</body>
<script src="<%= request.getContextPath() %>/assets/js/post-edit.js"></script>
</html>
