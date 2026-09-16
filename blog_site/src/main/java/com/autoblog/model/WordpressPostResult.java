package com.autoblog.model;

public class WordpressPostResult {
    private long id;
    private String link;
    private String status;
    private Long featuredMediaId;
    private String featuredMediaUrl;

    public WordpressPostResult() {}

    public WordpressPostResult(long id, String link, String status) {
        this.id = id;
        this.link = link;
        this.status = status;
    }

    public WordpressPostResult(long id, String link, String status, Long featuredMediaId, String featuredMediaUrl) {
        this.id = id;
        this.link = link;
        this.status = status;
        this.featuredMediaId = featuredMediaId;
        this.featuredMediaUrl = featuredMediaUrl;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getFeaturedMediaId() { return featuredMediaId; }
    public void setFeaturedMediaId(Long featuredMediaId) { this.featuredMediaId = featuredMediaId; }
    public String getFeaturedMediaUrl() { return featuredMediaUrl; }
    public void setFeaturedMediaUrl(String featuredMediaUrl) { this.featuredMediaUrl = featuredMediaUrl; }
}
