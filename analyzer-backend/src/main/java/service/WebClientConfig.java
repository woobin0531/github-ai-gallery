package com.myproject.analyzerbackend.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value; // 1. Value 임포트
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${GITHUB_API_TOKEN:#{null}}")
    private String githubToken;
    
    @Value("${OLLAMA_BASE_URL:http://localhost:11435}") 
    private String ollamaBaseUrl;

    @Value("${COMFYUI_BASE_URL:http://localhost:8189}") 
    private String comfyuiBaseUrl;

    // 1. Ollama용 WebClient
    @Bean
    @Qualifier("ollamaWebClient")
    public WebClient ollamaWebClient() {
        return WebClient.builder()
                .baseUrl(ollamaBaseUrl + "/api") // 3. 하드코딩된 URL 대신 변수 사용
                .build();
    }

    // 2. ComfyUI용 WebClient
    @Bean
    @Qualifier("comfyuiWebClient")
    public WebClient comfyuiWebClient() {
        return WebClient.builder()
                .baseUrl(comfyuiBaseUrl) // 3. 하드코딩된 URL 대신 변수 사용
                .build();
    }
    
    // 3. GitHub API용 WebClient (이건 수정 없음)
     @Bean
     @Qualifier("githubWebClient")
     public WebClient githubWebClient() {
     WebClient.Builder builder = WebClient.builder()
     .baseUrl("https://api.github.com")
         .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.v3+json");

     if (StringUtils.hasText(githubToken)) {
         System.out.println(">>> GitHub API Token 사용됨.");
         builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + githubToken);
        } else {
             System.out.println(">>> GitHub API Token 없음 (익명 모드 / 요청 제한 주의).");
     }

         return builder.build();
    }
}