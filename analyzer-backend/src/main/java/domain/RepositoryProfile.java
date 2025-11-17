package com.myproject.analyzerbackend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "project_analyzer_storage")
public class RepositoryProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String repoName; // 예: "airbnb/javascript" (고유 식별자)

    @Column
    private String projectTitle; // "Airbnb JavaScript Style Guide"

    private String repoUrl; // GitHub 저장소 URL

    @Column(columnDefinition = "TEXT")
    private String techStackSummary; // Ollama가 분석한 프로젝트 요약

    @Column
    private String topic; // 예: "AI Agent", "Docker", "RAG"
    private String imageUrl; // ComfyUI가 생성한 시각화 이미지 URL

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 분석 정보 생성 시간

}