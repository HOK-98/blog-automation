package com.autoblog.model;

public class WordpressSite {
    private long id;
    private String name;
    private String description;
    private String siteUrl;
    private String postsApiUrl;
    private String adminEmail;
    private String username;
    private String applicationPassword;
    private String timezone;
    private String language;
    private String defaultPostStatus;
    private String seoTitleSuffix;
    private String metaDescriptionTemplate;
    private boolean autoKeywordCollection;
    private boolean autoContentGeneration;
    private boolean autoPublishing;
    private boolean scheduleEnabled;
    private String scheduleStartTime;
    private int scheduledKeywordCount;
    private int scheduledPostCount;
    private String scheduleCategory;
    private java.sql.Date lastScheduledRunDate;
    private boolean notificationEnabled;
    private String notificationEmail;
    private boolean notifyOnPublish;
    private boolean autoImageGeneration;
    private boolean autoInternalLinks;
    private boolean active;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSiteUrl() { return siteUrl; }
    public void setSiteUrl(String siteUrl) { this.siteUrl = siteUrl; }
    public String getPostsApiUrl() { return postsApiUrl; }
    public void setPostsApiUrl(String postsApiUrl) { this.postsApiUrl = postsApiUrl; }
    public String getAdminEmail() { return adminEmail; }
    public void setAdminEmail(String adminEmail) { this.adminEmail = adminEmail; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getApplicationPassword() { return applicationPassword; }
    public void setApplicationPassword(String applicationPassword) { this.applicationPassword = applicationPassword; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getDefaultPostStatus() { return defaultPostStatus; }
    public void setDefaultPostStatus(String defaultPostStatus) { this.defaultPostStatus = defaultPostStatus; }
    public String getSeoTitleSuffix() { return seoTitleSuffix; }
    public void setSeoTitleSuffix(String seoTitleSuffix) { this.seoTitleSuffix = seoTitleSuffix; }
    public String getMetaDescriptionTemplate() { return metaDescriptionTemplate; }
    public void setMetaDescriptionTemplate(String metaDescriptionTemplate) { this.metaDescriptionTemplate = metaDescriptionTemplate; }
    public boolean isAutoKeywordCollection() { return autoKeywordCollection; }
    public void setAutoKeywordCollection(boolean autoKeywordCollection) { this.autoKeywordCollection = autoKeywordCollection; }
    public boolean isAutoContentGeneration() { return autoContentGeneration; }
    public void setAutoContentGeneration(boolean autoContentGeneration) { this.autoContentGeneration = autoContentGeneration; }
    public boolean isAutoPublishing() { return autoPublishing; }
    public void setAutoPublishing(boolean autoPublishing) { this.autoPublishing = autoPublishing; }
    public boolean isScheduleEnabled() { return scheduleEnabled; }
    public void setScheduleEnabled(boolean scheduleEnabled) { this.scheduleEnabled = scheduleEnabled; }
    public String getScheduleStartTime() { return scheduleStartTime; }
    public void setScheduleStartTime(String scheduleStartTime) { this.scheduleStartTime = scheduleStartTime; }
    public int getScheduledKeywordCount() { return scheduledKeywordCount; }
    public void setScheduledKeywordCount(int scheduledKeywordCount) { this.scheduledKeywordCount = scheduledKeywordCount; }
    public int getScheduledPostCount() { return scheduledPostCount; }
    public void setScheduledPostCount(int scheduledPostCount) { this.scheduledPostCount = scheduledPostCount; }
    public String getScheduleCategory() { return scheduleCategory; }
    public void setScheduleCategory(String scheduleCategory) { this.scheduleCategory = scheduleCategory; }
    public java.sql.Date getLastScheduledRunDate() { return lastScheduledRunDate; }
    public void setLastScheduledRunDate(java.sql.Date lastScheduledRunDate) { this.lastScheduledRunDate = lastScheduledRunDate; }
    public boolean isNotificationEnabled() { return notificationEnabled; }
    public void setNotificationEnabled(boolean notificationEnabled) { this.notificationEnabled = notificationEnabled; }
    public String getNotificationEmail() { return notificationEmail; }
    public void setNotificationEmail(String notificationEmail) { this.notificationEmail = notificationEmail; }
    public boolean isNotifyOnPublish() { return notifyOnPublish; }
    public void setNotifyOnPublish(boolean notifyOnPublish) { this.notifyOnPublish = notifyOnPublish; }
    public boolean isAutoImageGeneration() { return autoImageGeneration; }
    public void setAutoImageGeneration(boolean autoImageGeneration) { this.autoImageGeneration = autoImageGeneration; }
    public boolean isAutoInternalLinks() { return autoInternalLinks; }
    public void setAutoInternalLinks(boolean autoInternalLinks) { this.autoInternalLinks = autoInternalLinks; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
