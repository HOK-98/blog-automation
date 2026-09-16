document.addEventListener("DOMContentLoaded", function () {
    var editor = document.getElementById("editor");
    var contentField = document.getElementById("contentField");
    var form = document.getElementById("draftForm") || document.querySelector("form");
    var toolbarButtons = document.querySelectorAll("[data-command]");
    var linkButton = document.getElementById("linkButton");
    var categorySelect = document.getElementById("wordpressCategorySelect");
    var categoryNameField = document.getElementById("categoryNameField");

    toolbarButtons.forEach(function (button) {
        button.addEventListener("click", function () {
            var command = button.getAttribute("data-command");
            var value = button.getAttribute("data-value");
            document.execCommand(command, false, value || null);
            editor.focus();
        });
    });

    if (linkButton) {
        linkButton.addEventListener("click", function () {
            var url = window.prompt("링크 주소를 입력하세요.");
            if (url) {
                document.execCommand("createLink", false, url);
                editor.focus();
            }
        });
    }

    if (form && editor && contentField) {
        form.addEventListener("submit", function () {
            contentField.value = editor.innerHTML;
            if (categorySelect && categoryNameField) {
                var selected = categorySelect.options[categorySelect.selectedIndex];
                categoryNameField.value = selected ? (selected.getAttribute("data-name") || selected.text || "").trim() : "";
            }
        });
    }
});
