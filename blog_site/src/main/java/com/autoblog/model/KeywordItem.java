package com.autoblog.model;

import java.time.LocalDateTime;

public class KeywordItem {
    private long id;
    private long siteId;
    private String keyword;
    private String groupName;
    private String status;
    private LocalDateTime createdAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getSiteId() { return siteId; }
    public void setSiteId(long siteId) { this.siteId = siteId; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
