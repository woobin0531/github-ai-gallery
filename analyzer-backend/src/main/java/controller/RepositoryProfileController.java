package com.myproject.analyzerbackend.controller;

import com.myproject.analyzerbackend.domain.RepositoryProfile;
import com.myproject.analyzerbackend.domain.RepositoryProfileRepository;
import com.myproject.analyzerbackend.service.CrawlingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "http://localhost:5173")
public class RepositoryProfileController {

    @Autowired
    private RepositoryProfileRepository repositoryProfileRepository;

    @Autowired
    private CrawlingService crawlingService;

    // 모든 프로젝트 조회
    @GetMapping
    public ResponseEntity<Page<RepositoryProfile>> getAllProfiles(Pageable pageable) {
        Page<RepositoryProfile> profiles = repositoryProfileRepository.findAll(pageable);
        return ResponseEntity.ok(profiles);
    }

    // 키워드 검색
    @GetMapping("/search")
    public ResponseEntity<Page<RepositoryProfile>> searchProfiles(
            @RequestParam String keyword,
            Pageable pageable) {

        Page<RepositoryProfile> profiles = repositoryProfileRepository.findByProjectTitleContainingIgnoreCaseOrTechStackSummaryContainingIgnoreCase(
                keyword, keyword, pageable
        );
        return ResponseEntity.ok(profiles);
    }

    // 주제별 필터링
    @GetMapping("/filter")
    public ResponseEntity<Page<RepositoryProfile>> filterProfilesByTopic(
            @RequestParam String topic,
            Pageable pageable) {

        Page<RepositoryProfile> profiles = repositoryProfileRepository.findByTopic(topic, pageable);
        return ResponseEntity.ok(profiles);
    }

    // 사이드바용 토픽 목록 조회
    @GetMapping("/topics")
    public ResponseEntity<List<String>> getDistinctTopics() {
        List<String> topics = repositoryProfileRepository.findDistinctTopics();
        return ResponseEntity.ok(topics);
    }

    // 즉시 분석 요청
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, String>> requestImmediateAnalysis(@RequestBody Map<String, String> payload) {
        String url = payload.get("url");
        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "URL이 필요합니다."));
        }
        crawlingService.analyzeSingleUrl(url);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("message", "분석 요청이 접수되었습니다. 잠시 후 확인해주세요."));
    }

    // 내 보관소(즐겨찾기) 조회
    @PostMapping("/favorites")
    public ResponseEntity<Page<RepositoryProfile>> getFavoriteProfiles(
            @RequestBody List<Long> favoriteIds,
            Pageable pageable) {

        Page<RepositoryProfile> profiles = repositoryProfileRepository.findByIdIn(favoriteIds, pageable);
        return ResponseEntity.ok(profiles);
    }

    // 프로젝트 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProfile(@PathVariable Long id) {
        try {
            repositoryProfileRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}