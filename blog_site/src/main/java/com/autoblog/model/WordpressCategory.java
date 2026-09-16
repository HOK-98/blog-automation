package com.autoblog.model;

public class WordpressCategory {
    private long id;
    private String name;
    private String slug;
    private int count;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
}
