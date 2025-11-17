package com.myproject.analyzerbackend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;
import java.util.regex.Pattern;

record ProjectAnalysisResult(String projectTitle, String projectSummary, String imageConcept) {}

@Service
public class OllamaService {

    private final WebClient ollamaWebClient;
    private final ObjectMapper objectMapper;
    private final String model = "llama3";

    private static final int MAX_README_LENGTH = 8192;
    private static final Pattern CJK_PATTERN = Pattern.compile("[\\u4E00-\\u9FFF\\u3040-\\u30FF\\uFF00-\\uFFEF]");

    public OllamaService(@Qualifier("ollamaWebClient") WebClient ollamaWebClient, ObjectMapper objectMapper) {
        this.ollamaWebClient = ollamaWebClient;
        this.objectMapper = objectMapper;
    }

    public ProjectAnalysisResult analyzeReadme(String readmeContent) {
        if (readmeContent == null || readmeContent.isBlank()) {
            return null;
        }

        System.out.println(">>> Ollama 분석 시작 (최대 " + MAX_README_LENGTH + "자)");

        String contentToAnalyze = readmeContent;
        if (readmeContent.length() > MAX_README_LENGTH) {
            contentToAnalyze = readmeContent.substring(0, MAX_README_LENGTH) + "...";
        }

        String projectTitle = extractProjectTitle(contentToAnalyze);
        String projectSummary = extractProjectSummary(contentToAnalyze);
        String imageConcept = extractImageConcept(contentToAnalyze);

        if (projectTitle == null && projectSummary == null && imageConcept == null) {
            return null;
        }

        return new ProjectAnalysisResult(projectTitle, projectSummary, imageConcept);
    }

    private String extractProjectTitle(String content) {
        System.out.println("  >>> 프로젝트 제목 추출 중...");
        String prompt = "Extract the official H1(#) title from this GitHub README text. Respond with ONLY the title text. No markdown, no explanations.\n\n" + content;
        return callOllamaApi(prompt);
    }

    private String extractProjectSummary(String content) {
        System.out.println("  >>> 프로젝트 요약 추출 중...");

        // 프롬프트 강화: 한글 응답 및 잡담 제거
        String prompt = "You are a strict summarizer. Read the GitHub README below and summarize 'what this project is' in ONE Korean sentence (around 70 characters).\n"
                + "Rules:\n"
                + "1. MUST respond in KOREAN only.\n"
                + "2. Do NOT start with 'This project is...' or 'Based on...'.\n"
                + "3. Just output the summary directly.\n"
                + "\n"
                + "--- README ---\n"
                + content;

        String response = callOllamaApi(prompt);

        if (response != null) {
            return CJK_PATTERN.matcher(response.replaceAll("\"", "")).replaceAll("");
        }
        return null;
    }

    private String extractImageConcept(String content) {
        System.out.println("  >>> 이미지 장면(Scene) 묘사 추출 중...");

        // AI에게 구체적인 장면 묘사 요청
        String prompt = "Based on this GitHub README, describe a concrete 'Visual Scene' to generate a cover image.\n"
                + "Instructions:\n"
                + "1. If it's a GAME: Describe the main character, enemy, or gameplay action (e.g., 'A cute pixel knight fighting a dragon', 'A spaceship flying in stars').\n"
                + "2. If it's a WEB/APP: Describe the UI or screen (e.g., 'A mobile phone showing a chat app', 'A clean dashboard monitor with charts').\n"
                + "3. If it's a LIBRARY/TOOL: Describe a physical object or mascot representing it (e.g., 'A robot arm coding', 'A glowing database server').\n"
                + "\n"
                + "IMPORTANT: Respond with ONLY the English description (max 15 words). Do NOT use abstract words like 'future, abstract, connection'. Be specific!\n\n"
                + "--- README Start ---\n"
                + content;

        String response = callOllamaApi(prompt);

        if (response == null || response.isBlank()) {
            System.err.println("    - 이미지 컨셉 추출 실패 (기본값 사용)");
            return "A futuristic computer terminal with glowing code";
        }

        System.out.println("    - 이미지 장면 묘사 성공: " + response);
        return response.trim().replaceAll("\"", "");
    }

    public String translateToEnglish(String koreanText) {
        String prompt = "Translate this Korean text to simple English. Only the translation.\n\n" + koreanText;
        return callOllamaApi(prompt);
    }

    private String callOllamaApi(String prompt) {
        Map<String, Object> requestBody = Map.of("model", model, "prompt", prompt, "stream", false);
        try {
            String jsonResponse = ollamaWebClient.post().uri("/generate")
                    .contentType(MediaType.APPLICATION_JSON).bodyValue(requestBody).retrieve()
                    .bodyToMono(String.class).timeout(Duration.ofMinutes(3)).block();
            return extractResponseText(jsonResponse);
        } catch (Exception e) { return null; }
    }

    private String extractResponseText(String jsonResponse) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            return rootNode.path("response").asText("");
        } catch (JsonProcessingException e) { return ""; }
    }
}