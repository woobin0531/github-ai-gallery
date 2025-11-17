package com.myproject.analyzerbackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// 검색 결과와 주제를 담는 레코드
record GitHubSearchResult(String topic, List<JsonNode> repositories) {}

@Service
public class GitHubService {

    private final WebClient gitHubWebClient;
    private final ObjectMapper objectMapper;

    // [V42] 24개 핫한 주제 목록
    private static final List<String> SEARCH_TOPICS = List.of(
            "AI Agent", "LLM", "RAG", "Vector Database", "Langchain", "Ollama", "Stable Diffusion",
            "Data Engineering", "Microservices", "Self-Hosting", "DevOps", "Serverless", "GraphQL", "WebAssembly", "Rust", "Golang",
            "Docker", "Kubernetes", "Terraform", "Spring Boot", "Next.js", "Django", "Flutter", "FastAPI"
    );

    private static final AtomicInteger topicIndex = new AtomicInteger(0);

    // [V53] 주제별 페이지 번호 기억 장치
    private final Map<String, Integer> topicPageMap = new ConcurrentHashMap<>();

    public GitHubService(@Qualifier("githubWebClient") WebClient gitHubWebClient,
                         ObjectMapper objectMapper) {
        this.gitHubWebClient = gitHubWebClient;
        this.objectMapper = objectMapper;
    }

    /**
     * GitHub API로 인기 저장소를 검색합니다.
     */
    public GitHubSearchResult searchRepositories() {
        // 1. 이번에 검색할 주제 선택 (순환)
        int currentTopicIdx = topicIndex.getAndIncrement() % SEARCH_TOPICS.size();
        String topic = SEARCH_TOPICS.get(currentTopicIdx);

        // 2. 이 주제의 '현재 페이지' 가져오기 (기록 없으면 1페이지부터)
        int page = topicPageMap.getOrDefault(topic, 1);

        System.out.println(">>> GitHub API: 인기 저장소 검색 시작... (주제: \"" + topic + "\", 페이지: " + page + ")");

        String apiUrl = String.format(
                "/search/repositories?q=%s&sort=stars&order=desc&per_page=5&page=%d",
                topic, page
        );

        // 3. '다음 페이지' 미리 계산해서 메모장에 저장 (최대 10페이지까지만 보고 다시 1로 리셋)
        int nextPage = page + 1;
        if (nextPage > 10) nextPage = 1;
        topicPageMap.put(topic, nextPage);

        try {
            String jsonResponse = gitHubWebClient.get().uri(apiUrl)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve().bodyToMono(String.class).block();

            if (jsonResponse != null) {
                JsonNode root = objectMapper.readTree(jsonResponse);
                JsonNode items = root.path("items");
                if (items.isArray() && items.size() > 0) {
                    System.out.println(">>> GitHub API: 저장소 " + items.size() + "개 검색 완료.");
                    List<JsonNode> repositories = new ArrayList<>();
                    items.forEach(repositories::add);

                    return new GitHubSearchResult(topic, repositories);
                } else {
                    System.out.println(">>> GitHub API: 검색된 저장소 없음.");
                }
            }
        } catch (Exception e) {
            System.err.println(">>> GitHub API 검색 중 오류 발생: " + e.getMessage());
        }

        return new GitHubSearchResult(topic, Collections.emptyList());
    }

    /**
     * GitHub API로 README 콘텐츠를 가져옵니다.
     */
    public String getReadmeContent(String owner, String repoName) {
        System.out.println(">>> GitHub API: README 요청 - " + owner + "/" + repoName);
        String apiUrl = String.format("/repos/%s/%s/readme", owner, repoName);
        try {
            String jsonResponse = gitHubWebClient.get().uri(apiUrl)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve().bodyToMono(String.class).block();

            if (jsonResponse != null) {
                JsonNode root = objectMapper.readTree(jsonResponse);
                String contentBase64 = root.path("content").asText(null);
                if (contentBase64 != null) {
                    return decodeBase64(contentBase64);
                }
            }
        } catch (Exception e) {
            System.err.println(">>> README 요청 중 오류: " + e.getMessage());
        }
        return null;
    }

    /**
     * Base64 디코딩 헬퍼
     */
    private String decodeBase64(String base64Content) {
        try {
            String cleanBase64 = base64Content.replaceAll("\\s", "");
            byte[] decodedBytes = Base64.getDecoder().decode(cleanBase64);
            return new String(decodedBytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            System.err.println("Base64 디코딩 실패: " + e.getMessage());
            return null;
        }
    }
}