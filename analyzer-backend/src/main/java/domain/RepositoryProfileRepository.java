package com.myproject.analyzerbackend.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepositoryProfileRepository extends JpaRepository<RepositoryProfile, Long> {

    // (중복 저장 방지용)
    Optional<RepositoryProfile> findByRepoName(String repoName);

    // [V40-A] "검색창" 기능
    Page<RepositoryProfile> findByProjectTitleContainingIgnoreCaseOrTechStackSummaryContainingIgnoreCase(
            String keywordForTitle,
            String keywordForSummary,
            Pageable pageable
    );

    // [V40-B] "분류(필터)" 기능
    Page<RepositoryProfile> findByTopic(String topic, Pageable pageable);

    // [V40-E] "동적 사이드바"를 위한 쿼리
    @Query("SELECT DISTINCT r.topic FROM RepositoryProfile r WHERE r.topic IS NOT NULL")
    List<String> findDistinctTopics();
    Page<RepositoryProfile> findByIdIn(List<Long> ids, Pageable pageable);
}