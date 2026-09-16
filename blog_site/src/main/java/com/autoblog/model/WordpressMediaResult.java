package com.autoblog.model;

public class WordpressMediaResult {
    private long id;
    private String sourceUrl;
    private String altText;
    private String caption;
    private String externalSourceUrl;
    private String externalSourceTitle;

    public WordpressMediaResult() {}

    public WordpressMediaResult(long id, String sourceUrl, String altText) {
        this.id = id;
        this.sourceUrl = sourceUrl;
        this.altText = altText;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public String getAltText() { return altText; }
    public void setAltText(String altText) { this.altText = altText; }
    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
    public String getExternalSourceUrl() { return externalSourceUrl; }
    public void setExternalSourceUrl(String externalSourceUrl) { this.externalSourceUrl = externalSourceUrl; }
    public String getExternalSourceTitle() { return externalSourceTitle; }
    public void setExternalSourceTitle(String externalSourceTitle) { this.externalSourceTitle = externalSourceTitle; }
}
