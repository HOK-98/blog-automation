package com.autoblog.model;

public class PageViewMatchRow {
    private final String title;
    private final String wordpressUrl;
    private final String wordpressPath;
    private final String matchedGa4Path;
    private final long ga4Views;
    private final boolean matched;

    public PageViewMatchRow(String title, String wordpressUrl, String wordpressPath,
                            String matchedGa4Path, long ga4Views, boolean matched) {
        this.title = title;
        this.wordpressUrl = wordpressUrl;
        this.wordpressPath = wordpressPath;
        this.matchedGa4Path = matchedGa4Path;
        this.ga4Views = ga4Views;
        this.matched = matched;
    }

    public String getTitle() { return title; }
    public String getWordpressUrl() { return wordpressUrl; }
    public String getWordpressPath() { return wordpressPath; }
    public String getMatchedGa4Path() { return matchedGa4Path; }
    public long getGa4Views() { return ga4Views; }
    public boolean isMatched() { return matched; }
}
