package com.autoblog.model;

public class OfficialImageSource {
    private String imageUrl;
    private String sourceUrl;
    private String sourceTitle;
    private String caption;
    private String alt;
    private String license;

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public String getSourceTitle() { return sourceTitle; }
    public void setSourceTitle(String sourceTitle) { this.sourceTitle = sourceTitle; }
    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
    public String getAlt() { return alt; }
    public void setAlt(String alt) { this.alt = alt; }
    public String getLicense() { return license; }
    public void setLicense(String license) { this.license = license; }
}
