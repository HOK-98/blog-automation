package com.autoblog.service;

import com.autoblog.model.GeneratedDraft;
import com.autoblog.model.GeneratedImage;
import com.autoblog.model.WordpressSite;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

public class OpenAiImageGenerationServiceImpl implements ImageGenerationService {
    private static final String IMAGES_API_URL = "https://api.openai.com/v1/images/generations";
    private static final String USER_AGENT = "AutoBlog/1.0 Image Generator";

    private String envFilePath;
    private String apiKey;
    private String imageModel = "gpt-image-1";
    private String imageSize = "1024x1024";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void setEnvFilePath(String envFilePath) { this.envFilePath = envFilePath; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public void setImageModel(String imageModel) { if (notEmpty(imageModel)) this.imageModel = imageModel; }
    public void setImageSize(String imageSize) { if (notEmpty(imageSize)) this.imageSize = imageSize; }

    @Override
    public GeneratedImage generateFeaturedImage(WordpressSite site, GeneratedDraft draft) {
        return generateImage(site, draft, null, 1);
    }

    @Override
    public GeneratedImage generateInlineImage(WordpressSite site, GeneratedDraft draft, String sectionTitle, int index) {
        return generateImage(site, draft, sectionTitle, index);
    }

    private GeneratedImage generateImage(WordpressSite site, GeneratedDraft draft, String sectionTitle, int index) {
        String key = resolveApiKey();
        if (!notEmpty(key)) {
            throw new IllegalStateException("OPENAI_API_KEY가 설정되어 있지 않아 이미지 생성을 건너뜁니다.");
        }

        try {
            String prompt = buildPrompt(site, draft, sectionTitle, index);
            URL url = new URL(IMAGES_API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(90000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Authorization", "Bearer " + key);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", USER_AGENT);

            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("model", imageModel);
            payload.put("prompt", prompt);
            payload.put("size", imageSize);
            payload.put("n", 1);

            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(objectMapper.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8));
            }

            int code = connection.getResponseCode();
            String response = readBody(code >= 400 ? connection.getErrorStream() : connection.getInputStream());
            if (code < 200 || code >= 300) {
                throw new IllegalStateException("OpenAI 이미지 생성 실패: HTTP " + code + " / " + trimForLog(response));
            }

            JsonNode first = objectMapper.readTree(response).path("data").path(0);
            String b64 = first.path("b64_json").asText("");
            if (!notEmpty(b64)) {
                throw new IllegalStateException("OpenAI 이미지 응답에 b64_json이 없습니다.");
            }
            byte[] bytes = Base64.getDecoder().decode(b64);
            return new GeneratedImage(bytes, buildFilename(draft, index), buildAltText(draft, sectionTitle), prompt);
        } catch (Exception e) {
            throw new IllegalStateException("이미지 생성에 실패했습니다: " + rootMessage(e), e);
        }
    }

    private String buildPrompt(WordpressSite site, GeneratedDraft draft, String sectionTitle, int index) {
        String category = draft.getCategories() == null ? "" : draft.getCategories();
        String title = safeTitle(draft);
        String niche = site == null || site.getDescription() == null ? "" : site.getDescription();
        String section = notEmpty(sectionTitle) ? sectionTitle.trim() : title;
        return "Create a clean editorial illustration that helps readers understand one section of a Korean WordPress article. "
                + "Topic/title: " + title + ". "
                + "Section to illustrate: " + section + ". "
                + "Category/context: " + category + ". "
                + "Site context: " + niche + ". "
                + "Style: modern, high-quality, natural, web magazine inline illustration, informative but not cluttered, "
                + "no text, no letters, no logos, no watermark, no UI screenshots, no copyrighted characters, safe for general audiences. "
                + "Use a simple composition that visually supports the section rather than acting like a generic hero image.";
    }

    private String buildFilename(GeneratedDraft draft, int index) {
        String base = safeTitle(draft).toLowerCase()
                .replaceAll("[^a-z0-9가-힣]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        if (base.length() == 0) {
            base = "autoblog-image";
        }
        if (base.length() > 70) {
            base = base.substring(0, 70).replaceAll("-$", "");
        }
        return base + "-section-" + Math.max(1, index) + ".png";
    }

    private String buildAltText(GeneratedDraft draft, String sectionTitle) {
        if (notEmpty(sectionTitle)) {
            return safeTitle(draft) + " - " + sectionTitle.trim();
        }
        return safeTitle(draft);
    }

    private String safeTitle(GeneratedDraft draft) {
        if (draft == null || draft.getTitle() == null || draft.getTitle().trim().isEmpty()) {
            return "AutoBlog article image";
        }
        return draft.getTitle().trim();
    }

    private String resolveApiKey() {
        if (notEmpty(apiKey)) return apiKey.trim();
        String fromEnv = System.getenv("OPENAI_API_KEY");
        if (notEmpty(fromEnv)) return fromEnv.trim();
        Properties properties = loadEnvProperties();
        String fromFile = properties.getProperty("OPENAI_API_KEY");
        return notEmpty(fromFile) ? fromFile.trim() : "";
    }

    private Properties loadEnvProperties() {
        Properties result = new Properties();
        if (!notEmpty(envFilePath)) return result;
        File file = new File(envFilePath);
        if (!file.exists()) return result;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) continue;
                int index = trimmed.indexOf('=');
                String name = trimmed.substring(0, index).trim();
                String value = trimmed.substring(index + 1).trim();
                if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                result.setProperty(name, value);
            }
        } catch (Exception e) {
            System.err.println("[AutoBlog image] .env 읽기 실패: " + e.getMessage());
        }
        return result;
    }

    private boolean notEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String readBody(InputStream inputStream) throws Exception {
        if (inputStream == null) return "";
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        return body.toString();
    }

    private String trimForLog(String body) {
        if (body == null) return "";
        String compact = body.replace("\r", " ").replace("\n", " ").trim();
        return compact.length() > 500 ? compact.substring(0, 500) + "..." : compact;
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        String message = throwable.getMessage();
        while (current.getCause() != null) {
            current = current.getCause();
            if (current.getMessage() != null && !current.getMessage().trim().isEmpty()) {
                message = current.getMessage();
            }
        }
        return message == null ? throwable.getClass().getSimpleName() : message;
    }
}
