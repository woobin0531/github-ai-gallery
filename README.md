<img width="715" alt="GitHub AI Gallery Banner" src="httpsimg.shields.io/badge/Tech-Spring%20Boot-6DB33F?style=for-the-badge&logo=springboot" /> <img width="715" alt="GitHub AI Gallery Banner" src="httpsimg.shields.io/badge/Tech-React-61DAFB?style=for-the-badge&logo=react" /> <img width="715" alt="GitHub AI Gallery Banner" src="httpsimg.shields.io/badge/Tech-Docker-2496ED?style=for-the-badge&logo=docker" /> <img width="715" alt="GitHub AI Gallery Banner" src="httpsimg.shields.io/badge/AI-Ollama-lightgray?style=for-the-badge&logo=llama" /> <img width="715" alt="GitHub AI Gallery Banner" src="httpsimg.shields.io/badge/AI-Stable%20Diffusion-blueviolet?style=for-the-badge" />

# 🎨 GitHub AI 갤러리

<details>
<summary>프로젝트 개요 </summary> <br>

개발자로서 매일 수많은 GitHub 저장소를 접하지만, **README만 읽고서는 프로젝트의 실제 가치나 핵심 기능을 파악하기 어렵습니다.** 특히 인기 있는 프로젝트들은 문서가 방대해 "이게 정확히 뭐하는 거지?"라는 의문이 들 때가 많았습니다. <br><br>

이 문제를 해결하기 위해, **GitHub 저장소를 AI로 분석하고 시각화하는 'AI 갤러리'**를 개발했습니다. <br><br>

백엔드 스케줄러가 1분마다 인기 GitHub 저장소를 수집하면, `Ollama(Llama 3)`가 README를 분석해 **핵심 요약**을 추출하고, `ComfyUI(SDXL)`가 프로젝트를 상징하는 **썸네일 이미지**를 생성합니다. 사용자는 React UI를 통해 AI가 정리해준 프로젝트 갤러리를 보며, 키워드 검색이나 주제별 필터링을 통해 새로운 영감을 얻을 수 있습니다.

</details>

<details>
  <summary>주요 기능 </summary> <br>

- **자동화된 분석 (스케줄링)**: 1분마다 GitHub API를 순환 호출하여 'AI Agent', 'RAG', 'Docker' 등 24개 핫 토픽의 인기 저장소를 수집합니다. <br><br>
- **LLM 텍스트 요약**: `Ollama(Llama 3)`가 수집된 README 텍스트를 분석하여, 프로젝트의 제목, 핵심 기능 요약(한글), 이미지 생성용 컨셉(영어)을 추출합니다. <br><br>
- **AI 이미지 생성**: `ComfyUI(SDXL)`가 LLM이 추출한 컨셉을 프롬프트로 받아, 프로젝트를 상징하는 고유한 썸네일 이미지를 생성합니다. <br><br>
- **즉시 분석 요청**: 사용자가 UI에서 GitHub URL을 직접 입력하면, 해당 저장소를 즉시 분석 큐에 등록하여 갤러리에 추가합니다. <br><br>
- **인터랙티브 갤러리 (React)**:
  - **검색**: 제목과 요약 내용 기반의 키워드 검색
  - **필터**: AI가 분류한 주제(Topic)별 필터링
  - **즐겨찾기**: `localStorage`를 활용한 즐겨찾기(내 보관소) 기능

</details>

<br>

# 📊 Sequence Diagram (Mermaid)

<details>
  <summary> 1. 자동 분석 스케줄링 (Backend → GitHub → Ollama → ComfyUI) </summary> <br>

```mermaid
sequenceDiagram
    participant Scheduler (Backend)
    participant GitHubService
    participant OllamaService
    participant ComfyUiService
    participant DB (PostgreSQL)

    Scheduler->>GitHubService: 1. analyzeRepositories()
    GitHubService->>GitHub API: 2. searchRepositories()
    GitHub API-->>GitHubService: 3. 인기 저장소 5개
    GitHubService->>GitHub API: 4. getReadmeContent()
    GitHub API-->>GitHubService: 5. README (Base64)
    GitHubService->>OllamaService: 6. analyzeReadme()
    OllamaService->>Ollama (Docker): 7. LLM 프롬프트 (요약, 컨셉)
    Ollama (Docker)-->>OllamaService: 8. 분석 텍스트
    GitHubService->>ComfyUiService: 9. generateImage()
    ComfyUiService->>ComfyUI (Docker): 10. POST /prompt (워크플로우 + 프롬프트)
    ComfyUI (Docker)-->>ComfyUiService: 11. 이미지 URL
    GitHubService->>DB: 12. repositoryProfileRepository.saveAll()
</details>

<details>   <summary> 2. 사용자 요청 처리 (Frontend ↔ Backend) </summary>


코드 스니펫

sequenceDiagram
    participant User
    participant Frontend (React)
    participant Backend (Spring)
    participant DB (PostgreSQL)

    Note over User, Frontend: (A: 갤러리 조회)
    User->>Frontend: 1. 페이지 접속 / 필터 클릭
    Frontend->>Backend: 2. GET /api/projects (Pageable)
    Backend->>DB: 3. findAll(pageable)
    DB-->>Backend: 4. Page<RepositoryProfile>
    Backend-->>Frontend: 5. 갤러리 목록 (JSON)
    Frontend->>User: 6. 갤러리 렌더링

    Note over User, Frontend: (B: 즉시 분석 요청)
    User->>Frontend: 7. GitHub URL 입력 및 '분석' 클릭
    Frontend->>Backend: 8. POST /api/projects/analyze (URL)
    Backend-->>Frontend: 9. "분석 요청 접수됨" (202 Accepted)
    Note over Backend: (CrawlingService.analyzeSingleUrl() @Async 실행)
</details>

🔗 기술 적용 내역
<details>   <summary>기술 적용 내역 (Tech Stack)</summary>

  본 프로젝트는 Spring Boot 백엔드와 React 프론트엔드로 구성되며, Docker Compose를 통해 모든 서비스를 한번에 관리하는 마이크로서비스 아키텍처(MSA)를 채택했습니다.

  ### 🖥️ 프론트엔드 (analyzer-frontend)

React: 컴포넌트 기반 UI 구축

Vite: 빠르고 모던한 프론트엔드 빌드 도구

Axios: 백엔드 API 비동기 통신

CSS Modules: 컴포넌트 스코프 스타일링 (Gird Layout, Flexbox)

⚙️ 백엔드 (analyzer-backend)
Spring Boot 3.3 / Java 17: API 서버

Spring Data JPA / PostgreSQL: 데이터 저장 및 조회

Spring WebFlux (WebClient): Ollama, ComfyUI, GitHub API 비동기/논블로킹 호출

Spring Scheduling (@Scheduled): 1분 주기 자동 분석 스케줄러

Spring Async (@Async): '즉시 분석' 기능의 비동기 처리

🤖 AI / DevOps (Docker)
Docker / Docker Compose: 전체 서비스(Frontend, Backend, AI, DB) 실행 환경 통합

Ollama (ollama/ollama): llama3 모델을 서빙하여 텍스트 분석

ComfyUI (python:3.12-slim + Custom): sd_xl_base_1.0.safetensors 모델을 로드하여 이미지 생성

</details>

🔧 문제 해결 사례
<details>   <summary>문제 해결 사례</summary>


AI 서비스 동기화 문제: 초기에는 AI 모델(Ollama, ComfyUI) 호출 시 응답이 올 때까지 메인 스레드가 대기(Blocking)하는 문제가 있었습니다.

해결: RestTemplate 대신 **Spring WebFlux의 WebClient**를 도입하여, 모든 외부 API 호출을 비동기/논블로킹 방식으로 처리했습니다. 이로써 스케줄러가 대기 없이 더 많은 작업을 효율적으로 처리할 수 있게 되었습니다.



'즉시 분석'의 UI 응답 지연: 사용자가 '즉시 분석'을 요청하면, AI 작업이 끝날 때까지 (최대 1~2분) UI가 응답을 받지 못하는 문제가 있었습니다.

해결: CrawlingService의 analyzeSingleUrl 메서드에 @Async 어노테이션을 적용했습니다. 컨트롤러는 요청을 받자마자 "접수됨(202) (Accepted)" 응답을 즉시 반환하고, 실제 분석 작업은 백그라운드 스레드에서 처리하도록 분리하여 사용자 경험(UX)을 개선했습니다.



복잡한 서비스 환경 관리: 개발 환경에서 Backend(Java), Frontend(Node), Ollama(Python), ComfyUI(Python) 등 4~5개의 다른 런타임과 터미널을 동시에 실행해야 했습니다.

해결: **docker-compose.yml**을 작성하여 모든 서비스를 하나의 명령(docker compose up)으로 관리할 수 있도록 마이크로서비스 아키텍처를 완성했습니다. 이는 다른 개발자가 프로젝트를 실행하는 과정(Getting Started)을 극단적으로 단순화시켰습니다.

</details>

🏁 시작하기 (Getting Started)
<details>   <summary>Getting Started (Docker Compose)</summary>


이 프로젝트는 docker-compose를 사용하여 단 하나의 명령어로 모든 서비스를 실행할 수 있도록 패키징되어 있습니다.

사전 요구 사항
Docker Desktop이 설치되어 있어야 합니다.

(권장) 최소 8GB 이상의 VRAM이 탑재된 NVIDIA GPU (ComfyUI의 AI 이미지 생성을 위해)

설치 및 실행
AI 모델 다운로드

LLM (Llama 3): Ollama가 자동으로 다운로드합니다. (docker-compose.yml에 llama3 모델을 사용하도록 이미 설정되어 있을 수 있습니다.)

Image (SDXL): sd_xl_base_1.0.safetensors 모델 파일을 다운로드하여 comfyui/models/checkpoints/ 디렉토리(또는 docker-compose.yml에 지정된 경로)에 배치해야 합니다.

프로젝트 클론

Bash

git clone [https://github.com/woobin0531/github-ai-gallery.git](https://github.com/woobin0531/github-ai-gallery.git)
cd github-ai-gallery
Docker Compose 실행

Bash

# (선택) 백엔드, 프론트엔드 이미지 빌드
docker compose build

# 모든 서비스 시작 (백그라운드 실행)
docker compose up -d
접속 확인

Frontend (갤러리): http://localhost:5173

Backend (API): http://localhost:8080

ComfyUI (워크플로우): http://localhost:8189

Ollama (API): http://localhost:11435

</details>
```
