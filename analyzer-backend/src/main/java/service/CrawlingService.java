package com.myproject.analyzerbackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.myproject.analyzerbackend.domain.RepositoryProfile;
import com.myproject.analyzerbackend.domain.RepositoryProfileRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@EnableAsync
@Service
public class CrawlingService {

    @Autowired
    private RepositoryProfileRepository repositoryProfileRepository;

    @Autowired
    private EntityManager entityManager;

    private final OllamaService ollamaService;
    private final ComfyUiService comfyUiService;
    private final GitHubService gitHubService;

    private static final int BATCH_SIZE = 5;
    private static final Pattern CJK_PATTERN = Pattern.compile("[\\u4E00-\\u9FFF\\u3040-\\u30FF]");
    private static final Pattern GITHUB_URL_PATTERN = Pattern.compile("https://github\\.com/([^/]+)/([^/]+)");

    // ComfyUI 부정 프롬프트
    private static final String NEGATIVE_PROMPT = "(worst quality, low quality, normal quality:2.0), (text, watermark, signature:1.5), (human, people, man, woman, face, realistic:2.0), (robot:1.5), (dog, cat, pet:1.5), blurry, deformed, nsfw";

    public CrawlingService(OllamaService ollamaService,
                           ComfyUiService comfyUiService,
                           GitHubService gitHubService) {
        this.ollamaService = ollamaService;
        this.comfyUiService = comfyUiService;
        this.gitHubService = gitHubService;
    }

    /**
     * 단일 GitHub URL 즉시 분석 (비동기)
     */
    @Async
    @Transactional
    public void analyzeSingleUrl(String githubUrl) {
        System.out.println("####### [즉시 분석] " + githubUrl + " 시작... #######");

        java.util.regex.Matcher matcher = GITHUB_URL_PATTERN.matcher(githubUrl);
        if (!matcher.find()) {
            System.err.println("  - 실패: 올바르지 않은 GitHub URL입니다.");
            return;
        }
        String owner = matcher.group(1);
        String repoName = matcher.group(2);
        String fullRepoName = owner + "/" + repoName;

        if (repositoryProfileRepository.findByRepoName(fullRepoName).isPresent()) {
            System.out.println("  - 이미 분석된 저장소입니다: " + fullRepoName);
            return;
        }

        String readmeContent = gitHubService.getReadmeContent(owner, repoName);
        if (readmeContent == null || readmeContent.isEmpty()) {
            System.out.println("  - README가 없습니다: " + fullRepoName);
            return;
        }

        ProjectAnalysisResult analysisResult = ollamaService.analyzeReadme(readmeContent);
        if (analysisResult == null) {
            System.err.println("  - Ollama 분석 실패: " + fullRepoName);
            return;
        }

        RepositoryProfile profile = new RepositoryProfile();
        profile.setRepoName(fullRepoName);
        profile.setRepoUrl(githubUrl);
        profile.setTopic("On-Demand");

        String title = analysisResult.projectTitle();
        String summary = analysisResult.projectSummary();
        String concept = analysisResult.imageConcept();

        profile.setProjectTitle(title != null ? title : fullRepoName);
        profile.setTechStackSummary(summary != null ? summary : "요약 추출 실패");

        String positivePrompt = createSuperPrompt(title, concept);

        if (positivePrompt != null) {
            try {
                String imageUrl = comfyUiService.generateImageForHotdeal(positivePrompt, NEGATIVE_PROMPT);
                profile.setImageUrl(imageUrl);
                System.out.println("  - 이미지 생성 성공: " + profile.getRepoName());
            } catch (Exception e) {
                System.err.println("    - 이미지 생성 오류: " + e.getMessage());
            }
        }

        repositoryProfileRepository.save(profile);
        System.out.println("####### [즉시 분석] 완료: " + fullRepoName + " #######");
    }

    /**
     * 이미지 재생성 (Re-generate)
     */
    @Async
    @Transactional
    public boolean regenerateImageForProfile(Long id) {
        System.out.println("####### [이미지 재생성] 시작 (ID: " + id + ") #######");

        Optional<RepositoryProfile> optionalProfile = repositoryProfileRepository.findById(id);
        if (optionalProfile.isEmpty()) {
            System.err.println("  - 실패: ID " + id + "를 찾을 수 없습니다.");
            return false;
        }
        RepositoryProfile profile = optionalProfile.get();

        String title = profile.getProjectTitle();
        if (title == null || title.isEmpty()) title = "Software Project";

        String [] styles = {
                "cyberpunk style, neon lights",
                "minimalist vector art, flat design",
                "3D render, unreal engine, isometric",
                "blueprint, technical drawing",
                "oil painting, artistic",
                "abstract tech visualization, blue nodes"
        };
        String randomStyle = styles[(int)(Math.random() * styles.length)];

        String positivePrompt = "masterpiece, best quality, 4k, " + randomStyle + ", " + title;

        try {
            String imageUrl = comfyUiService.generateImageForHotdeal(positivePrompt, NEGATIVE_PROMPT);
            profile.setImageUrl(imageUrl);
            repositoryProfileRepository.save(profile);
            System.out.println("  - 이미지 재생성 성공: " + profile.getRepoName());
            return true;
        } catch (Exception e) {
            System.err.println("  - 이미지 재생성 오류: " + e.getMessage());
            return false;
        }
    }

    /**
     * 자동 분석 스케줄러
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void analyzeRepositories() {
        if (!comfyUiService.isWorkflowLoaded()) {
            System.err.println(">>> ComfyUI 워크플로우 로드 실패"); return;
        }

        GitHubSearchResult searchResult = gitHubService.searchRepositories();
        String currentTopic = searchResult.topic();
        List<JsonNode> repositories = searchResult.repositories();

        if (repositories.isEmpty()) return;

        System.out.println(">>> 스케줄러 실행 (주제: " + currentTopic + ", 대상: " + repositories.size() + "개)");

        List<RepositoryProfile> profilesToSave = new ArrayList<>();

        for (JsonNode repo : repositories) {
            String owner = repo.path("owner").path("login").asText(null);
            String repoName = repo.path("name").asText(null);
            String repoUrl = repo.path("html_url").asText(null);
            String fullRepoName = owner + "/" + repoName;

            if (owner == null || repoName == null) continue;

            if (repositoryProfileRepository.findByRepoName(fullRepoName).isPresent()) continue;

            String readmeContent = gitHubService.getReadmeContent(owner, repoName);
            if (readmeContent == null || readmeContent.isEmpty()) continue;

            if (isReadmeNonKoreanOrEnglish(readmeContent, repo.path("language").asText(""))) continue;

            ProjectAnalysisResult analysisResult = ollamaService.analyzeReadme(readmeContent);
            if (analysisResult == null) continue;

            RepositoryProfile profile = new RepositoryProfile();
            profile.setRepoName(fullRepoName);
            profile.setRepoUrl(repoUrl);
            profile.setTopic(currentTopic);

            String title = analysisResult.projectTitle();
            String summary = analysisResult.projectSummary();
            String concept = analysisResult.imageConcept();

            profile.setProjectTitle(title != null ? title : repo.path("description").asText(fullRepoName));
            profile.setTechStackSummary(summary != null ? summary : "요약 추출 실패");

            String positivePrompt = createSuperPrompt(title, concept);

            if (positivePrompt != null) {
                try {
                    String imageUrl = comfyUiService.generateImageForHotdeal(positivePrompt, NEGATIVE_PROMPT);
                    profile.setImageUrl(imageUrl);
                } catch (Exception e) {
                    System.err.println("    - 이미지 생성 오류: " + e.getMessage());
                }
            }
            profilesToSave.add(profile);
        }

        if (!profilesToSave.isEmpty()) {
            repositoryProfileRepository.saveAll(profilesToSave);
            System.out.println(">>> " + profilesToSave.size() + "개 분석 완료 및 저장.");
        }
    }

    private String createSuperPrompt(String title, String conceptKeywords) {
        String cleanTitle = sanitizeForComfyUI(title);
        String cleanConcept = sanitizeForComfyUI(conceptKeywords);

        String superPrompt = Stream.of(
                        "masterpiece, best quality, 4k",
                        cleanConcept,
                        cleanTitle
                )
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.joining(", "));

        if (superPrompt.equals("masterpiece, best quality, 4k")) return null;
        return superPrompt;
    }

    private String sanitizeForComfyUI(String text) {
        if (text == null || text.isBlank()) return null;
        if (text.contains("추출 불가") || text.contains("요약 불가") || text.contains("컨셉 없음")) return null;

        String cleanedText = text
                .replaceAll("\\(.*?\\)", "")
                .replaceAll("\\[.*?\\]", "")
                .replaceAll("\\*\\*", "")
                .replaceAll("[\n\r]", " ")
                .replaceAll("\"", "'");

        cleanedText = cleanedText.replaceAll("[a-zA-Z\\uAC00-\\uD7AF ]+:", "");
        if (cleanedText.toLowerCase().contains("let me know")) cleanedText = cleanedText.replaceAll("(?i)Let me know [^,]+", "");
        if (cleanedText.toLowerCase().contains("here are")) cleanedText = cleanedText.replaceAll("(?i)Here are [^,]+", "");

        cleanedText = cleanedText.replaceAll("[\\p{So}]", "");
        cleanedText = cleanedText.trim().replaceAll("^,s*|s*,$", "").trim();

        return cleanedText.isEmpty() ? null : cleanedText;
    }

    private boolean isReadmeNonKoreanOrEnglish(String content, String repoLanguage) {
        if (repoLanguage != null && (repoLanguage.equalsIgnoreCase("Chinese") || repoLanguage.equalsIgnoreCase("Japanese"))) return true;
        if (content == null || content.isEmpty()) return false;
        String sample = content.substring(0, Math.min(content.length(), 1000));
        long cjkCount = CJK_PATTERN.matcher(sample).results().count();
        return (double) cjkCount / sample.length() > 0.1;
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanupOldData() {
        try {
            LocalDateTime threshold = LocalDateTime.now().minusDays(30);
            int deletedCount = entityManager.createQuery("DELETE FROM RepositoryProfile rp WHERE rp.createdAt < :threshold")
                    .setParameter("threshold", threshold).executeUpdate();
            System.out.println(">>> [청소] " + deletedCount + "개 삭제 완료.");
        } catch (Exception e) { e.printStackTrace(); }
    }
}