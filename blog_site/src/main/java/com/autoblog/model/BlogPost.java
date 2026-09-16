package com.autoblog.model;

import java.time.LocalDateTime;

public class BlogPost {
    private long id;
    private long siteId;
    private Long wordpressPostId;
    private String title;
    private String summary;
    private String content;
    private String category;
    private String status;
    private String tags;
    private String coverImageUrl;
    private String wordpressUrl;
    private long viewCount;
    private LocalDateTime publishedAt;
    private LocalDateTime updatedAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getSiteId() { return siteId; }
    public void setSiteId(long siteId) { this.siteId = siteId; }
    public Long getWordpressPostId() { return wordpressPostId; }
    public void setWordpressPostId(Long wordpressPostId) { this.wordpressPostId = wordpressPostId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
    public String getWordpressUrl() { return wordpressUrl; }
    public void setWordpressUrl(String wordpressUrl) { this.wordpressUrl = wordpressUrl; }
    public long getViewCount() { return viewCount; }
    public void setViewCount(long viewCount) { this.viewCount = viewCount; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
