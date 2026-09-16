package com.autoblog.model;

import java.util.List;

public class GenerationPreview {
    private String keyword;
    private String title;
    private String slug;
    private String excerpt;
    private String contentHtml;
    private List<String> tags;
    private List<String> categories;
    private boolean approved;
    private boolean criticalFactualError;
    private int score;
    private List<String> issues;
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;
    private String officialImages;

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getExcerpt() { return excerpt; }
    public void setExcerpt(String excerpt) { this.excerpt = excerpt; }
    public String getContentHtml() { return contentHtml; }
    public void setContentHtml(String contentHtml) { this.contentHtml = contentHtml; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }
    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }
    public boolean isCriticalFactualError() { return criticalFactualError; }
    public void setCriticalFactualError(boolean criticalFactualError) { this.criticalFactualError = criticalFactualError; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public List<String> getIssues() { return issues; }
    public void setIssues(List<String> issues) { this.issues = issues; }
    public int getPromptTokens() { return promptTokens; }
    public void setPromptTokens(int promptTokens) { this.promptTokens = promptTokens; }
    public int getCompletionTokens() { return completionTokens; }
    public void setCompletionTokens(int completionTokens) { this.completionTokens = completionTokens; }
    public int getTotalTokens() { return totalTokens; }
    public void setTotalTokens(int totalTokens) { this.totalTokens = totalTokens; }
    public String getOfficialImages() { return officialImages; }
    public void setOfficialImages(String officialImages) { this.officialImages = officialImages; }
}
