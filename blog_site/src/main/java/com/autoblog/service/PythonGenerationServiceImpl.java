package com.autoblog.service;

import com.autoblog.model.GenerationPreview;
import com.autoblog.util.EncodingUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class PythonGenerationServiceImpl implements GenerationService {
    private String pythonExecutable;
    private String scriptPath;
    private String workingDirectory;

    public void setPythonExecutable(String pythonExecutable) { this.pythonExecutable = pythonExecutable; }
    public void setScriptPath(String scriptPath) { this.scriptPath = scriptPath; }
    public void setWorkingDirectory(String workingDirectory) { this.workingDirectory = workingDirectory; }

    @Override
    public GenerationPreview generatePreview(String keyword) {
        try {
            // 실제 글 생성 품질 로직은 저장소의 automation/blog_automation.py에 있습니다.
            // Java는 해당 Python 스크립트를 실행하고 JSON 결과만 파싱합니다.
            ProcessBuilder builder = new ProcessBuilder(
                    pythonExecutable,
                    scriptPath,
                    "--preview-json",
                    keyword == null ? "" : keyword);
            builder.directory(new java.io.File(workingDirectory));
            // Windows 콘솔 기본 인코딩(CP949) 때문에 한글이 깨지는 것을 막기 위해
            // Python stdout을 UTF-8로 고정합니다.
            builder.environment().put("PYTHONIOENCODING", "utf-8");
            builder.environment().put("PYTHONUTF8", "1");
            builder.redirectErrorStream(true);
            Process process = builder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IllegalStateException("파이썬 생성 실행 실패: " + output.toString());
            }
            return parse(output.toString());
        } catch (Exception e) {
            throw new IllegalStateException("자동 생성 미리보기에 실패했습니다.", e);
        }
    }

    private GenerationPreview parse(String raw) throws Exception {
        // Python은 {"article": ..., "verification": ...} 형태의 JSON을 출력합니다.
        // 이 데이터를 화면 미리보기와 generated_drafts 저장에 쓰는 모델로 변환합니다.
        JsonNode root = new ObjectMapper().readTree(raw);
        JsonNode article = root.path("article");
        JsonNode verification = root.path("verification");

        GenerationPreview preview = new GenerationPreview();
        preview.setKeyword(EncodingUtil.repairMojibake(root.path("keyword").asText()));
        preview.setTitle(EncodingUtil.repairMojibake(article.path("title").asText()));
        preview.setSlug(EncodingUtil.repairMojibake(article.path("slug").asText()));
        preview.setExcerpt(EncodingUtil.repairMojibake(article.path("excerpt").asText()));
        preview.setContentHtml(EncodingUtil.repairMojibake(article.path("content_html").asText()));
        preview.setTags(toStringList(article.path("tags")));
        preview.setCategories(toStringList(article.path("categories")));
        preview.setApproved(root.path("approved").asBoolean());
        preview.setCriticalFactualError(root.path("critical_factual_error").asBoolean(false));
        preview.setScore(root.path("score").asInt());
        preview.setIssues(toStringList(verification.path("issues")));
        JsonNode officialImages = root.has("official_images") ? root.path("official_images") : verification.path("official_images");
        preview.setOfficialImages(officialImages.isMissingNode() || officialImages.isNull() ? "" : new ObjectMapper().writeValueAsString(officialImages));
        JsonNode usage = root.path("usage");
        preview.setPromptTokens(usage.path("prompt_tokens").asInt());
        preview.setCompletionTokens(usage.path("completion_tokens").asInt());
        preview.setTotalTokens(usage.path("total_tokens").asInt());
        return preview;
    }

    private List<String> toStringList(JsonNode node) {
        List<String> result = new ArrayList<String>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                result.add(EncodingUtil.repairMojibake(item.asText()));
            }
        }
        return result;
    }
}

