document.addEventListener("DOMContentLoaded", function () {
    var buttons = document.querySelectorAll(".tabs button");
    var panels = document.querySelectorAll(".tab-panel");

    buttons.forEach(function (button) {
        button.addEventListener("click", function () {
            var target = button.getAttribute("data-tab");
            buttons.forEach(function (item) {
                item.classList.remove("active");
            });
            panels.forEach(function (panel) {
                panel.classList.toggle("active", panel.getAttribute("data-panel") === target);
            });
            button.classList.add("active");
        });
    });
});
