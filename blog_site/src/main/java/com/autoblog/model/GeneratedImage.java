package com.autoblog.model;

public class GeneratedImage {
    private byte[] bytes;
    private String filename;
    private String altText;
    private String prompt;

    public GeneratedImage() {}

    public GeneratedImage(byte[] bytes, String filename, String altText, String prompt) {
        this.bytes = bytes;
        this.filename = filename;
        this.altText = altText;
        this.prompt = prompt;
    }

    public byte[] getBytes() { return bytes; }
    public void setBytes(byte[] bytes) { this.bytes = bytes; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public String getAltText() { return altText; }
    public void setAltText(String altText) { this.altText = altText; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
}
