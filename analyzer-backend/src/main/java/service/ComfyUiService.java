package com.myproject.analyzerbackend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

@Service
public class ComfyUiService {

    private final WebClient comfyuiWebClient;
    private final ObjectMapper objectMapper;
    private final String comfyBaseUrl = "http://localhost:8189";
    private String workflowJsonTemplate;

    // T2I 워크플로우의 노드 ID (github-profile-workflow.json 파일과 일치해야 함)
    private final String COMFY_POSITIVE_NODE_ID = "6";
    private final String COMFY_NEGATIVE_NODE_ID = "7";

    public ComfyUiService(@Qualifier("comfyuiWebClient") WebClient comfyuiWebClient, ObjectMapper objectMapper) {
        this.comfyuiWebClient = comfyuiWebClient;
        this.objectMapper = objectMapper;
        try {
            this.workflowJsonTemplate = loadWorkflowJson("github-profile-workflow.json");
            System.out.println(">>> ComfyUI 워크플로우 로드 성공.");
        } catch (Exception e) {
            System.err.println(">>> ComfyUI 워크플로우 로드 실패!");
            e.printStackTrace();
            this.workflowJsonTemplate = null;
        }
    }

    /**
     * JSON 워크플로우 파일을 로드합니다.
     */
    private String loadWorkflowJson(String path) throws Exception {
        Resource resource = new ClassPathResource(path);
        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        }
    }

    /**
     * 워크플로우 템플릿이 로드되었는지 확인합니다.
     */
    public boolean isWorkflowLoaded() {
        return this.workflowJsonTemplate != null;
    }

    /**
     * T2I (Text-to-Image)로 이미지를 생성합니다.
     */
    public String generateImageForHotdeal(String positivePrompt, String negativePrompt) throws Exception {
        if (!isWorkflowLoaded()) throw new Exception("ComfyUI 워크플로우가 로드되지 않았습니다.");

        JsonNode workflow = updateWorkflowPrompts(positivePrompt, negativePrompt);

        String promptId = submitWorkflowToComfyUI(workflow);
        if (promptId == null) {
            throw new Exception("ComfyUI API 제출 실패. Prompt: " + positivePrompt);
        }

        JsonNode historyBlock = pollComfyUIHistory(promptId);
        if (historyBlock == null) {
            throw new Exception("ComfyUI 작업 타임아웃. Prompt: " + positivePrompt + ", ID: " + promptId);
        }

        return extractImageUrlFromHistory(historyBlock);
    }

    /**
     * T2I 워크플로우 JSON의 프롬프트를 업데이트합니다.
     */
    private JsonNode updateWorkflowPrompts(String positivePrompt, String negativePrompt) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(workflowJsonTemplate);

        // Positive 프롬프트 업데이트
        JsonNode posNode = root.path(COMFY_POSITIVE_NODE_ID).path("inputs");
        if (posNode.isObject()) {
            ((ObjectNode) posNode).put("text", positivePrompt);
        } else {
            System.err.println("경고: ComfyUI Positive 노드(" + COMFY_POSITIVE_NODE_ID + ") 찾을 수 없음.");
        }

        // Negative 프롬프트 업데이트
        JsonNode negNode = root.path(COMFY_NEGATIVE_NODE_ID).path("inputs");
        if (negNode.isObject()) {
            ((ObjectNode) negNode).put("text", negativePrompt);
        } else {
            System.err.println("경고: ComfyUI Negative 노드(" + COMFY_NEGATIVE_NODE_ID + ") 찾을 수 없음.");
        }

        return root;
    }

    /**
     * ComfyUI /prompt API에 워크플로우를 제출합니다.
     */
    private String submitWorkflowToComfyUI(JsonNode workflow) {
        Map<String, JsonNode> body = Map.of("prompt", workflow);
        try {
            JsonNode response = comfyuiWebClient.post().uri("/prompt")
                    .contentType(MediaType.APPLICATION_JSON).bodyValue(body)
                    .retrieve().bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(10)).block();
            return response != null ? response.path("prompt_id").asText(null) : null;
        } catch (Exception e) {
            System.err.println("ComfyUI /prompt API 호출 중 오류: " + e.getMessage());
            return null;
        }
    }

    /**
     * ComfyUI /history API를 폴링하여 작업 완료를 기다립니다.
     */
    private JsonNode pollComfyUIHistory(String promptId) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long timeoutMillis = 180000; // 3분
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            try {
                JsonNode history = comfyuiWebClient.get().uri("/history/" + promptId)
                        .retrieve().bodyToMono(JsonNode.class)
                        .timeout(Duration.ofSeconds(5)).block();
                if (history != null && history.has(promptId) && !history.get(promptId).isEmpty()) {
                    return history.get(promptId);
                }
            } catch (Exception e) { /* 404, Timeout 무시 */ }
            Thread.sleep(2000);
        }
        System.err.println("ComfyUI 작업 타임아웃 (ID: " + promptId + ")");
        return null;
    }

    /**
     * 히스토리에서 이미지 URL을 추출합니다.
     */
    private String extractImageUrlFromHistory(JsonNode historyBlock) {
        if (historyBlock == null) return null;
        JsonNode outputs = historyBlock.path("outputs");
        if (!outputs.isObject()) return null;
        for (JsonNode node : outputs) {
            if (node.has("images")) {
                JsonNode images = node.get("images");
                if (images.isArray() && !images.isEmpty()) {
                    JsonNode firstImage = images.get(0);
                    String filename = firstImage.path("filename").asText(null);
                    String subfolder = firstImage.path("subfolder").asText("");
                    String type = firstImage.path("type").asText("output");
                    if (filename != null) {
                        return String.format("%s/view?filename=%s&subfolder=%s&type=%s", this.comfyBaseUrl, filename, subfolder, type);
                    }
                }
            }
        }
        return null;
    }
}