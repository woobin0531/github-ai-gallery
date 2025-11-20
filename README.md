# 🎨 GitHub AI Gallery (GitHub AI 요약기)

## 📌 프로젝트 개요

**GitHub AI Gallery**는 매일 쏟아지는 방대한 오픈소스 프로젝트들을 사용자가 직관적으로 파악할 수 있도록 돕는 **AI 기반 큐레이션 플랫폼**입니다.

사용자는 텍스트 위주의 딱딱한 `README.md` 대신, **LLM(Llama 3)이 요약한 핵심 내용**과 **ComfyUI(SDXL)가 생성한 고품질 커버 이미지**를 통해 프로젝트의 성격을 한눈에 파악할 수 있습니다.

---

## ⚙️ 기술 적용 내역

본 프로젝트는 **Java 17 & Spring Boot 3.3.1**을 기반으로 개발되었으며, 생성형 AI 모델의 효율적인 동작을 위해 **Docker Container 환경**을 구축했습니다.

### 🖥️ 프론트엔드

- **React 18 + Vite** — 빠른 빌드 속도와 SPA(Single Page Application) 구성
- **Custom Grid CSS** — 다양한 해상도에 대응하는 반응형 갤러리 레이아웃 구현
- **Axios** — 백엔드 API와의 비동기 통신 및 인터셉터 처리

### 🛠️ 백엔드 (Server API)

- **Spring Boot 3.3** — 안정적인 엔터프라이즈급 애플리케이션 구축
- **Spring WebClient** — GitHub/Ollama/ComfyUI API와의 **Non-blocking I/O** 통신 구현 (동기 방식 대비 리소스 효율 극대화)
- **Spring Data JPA (Hibernate)** — PostgreSQL 객체 매핑 및 트랜잭션 관리
- **Java Concurrency (@Async)** — 이미지 생성 등 긴 작업(Long-polling)의 비동기 백그라운드 처리

### 🤖 AI & 데이터 (Model & Infra)

- **Ollama (Llama 3)** — 로컬 LLM 구동을 통한 비용 절감 및 README 텍스트 분석 (요약/컨셉 추출)
- **ComfyUI (SDXL 1.0)** — 노드 기반의 워크플로우를 활용한 고품질 T2I(Text-to-Image) 생성
- **PostgreSQL 16** — 프로젝트 메타데이터 및 분석 결과 영구 저장
- **Docker Compose** — 다중 컨테이너(AI, DB, App) 오케스트레이션 및 GPU 리소스 할당(NVIDIA Container Toolkit)

---

## 🚀 주요 기능

### 1. 자동 큐레이션 (Auto-Curation)

- **동적 스케줄링:** 1분 주기로 실행되며 'AI Agent', 'RAG', 'Rust' 등 **24가지 최신 개발 트렌드**를 순환 수집합니다.
- **스마트 페이지네이션:** `topicPageMap`을 활용하여 중복 수집을 방지하고 다양한 저장소를 탐색합니다.

### 2. AI 분석 파이프라인 (Analysis Pipeline)

- **핵심 요약:** 방대한 README 문서를 Llama 3 모델이 분석하여 **"이 프로젝트가 무엇인지"** 한국어 한 문장으로 정제합니다.
- **시각적 컨셉 도출:** 프로젝트의 기술적 특성(Web, Library, CLI 등)을 분석하여 이미지 생성에 최적화된 **영어 프롬프트(Prompt)**를 추출합니다.

### 3. 생성형 이미지 (Generative Art)

- **ComfyUI 연동:** 추출된 프롬프트를 기반으로 SDXL 모델이 프로젝트의 분위기를 시각화한 썸네일을 생성합니다.
- **프롬프트 엔지니어링:** 불필요한 텍스트 생성을 억제하고(Negative Prompt), 시각적 퀄리티를 높이는 키워드를 자동 주입합니다.

### 4. 사용자 경험 (UX)

- **즉시 분석 (On-Demand):** 사용자가 원하는 GitHub URL 입력 시, 우선순위 큐를 통해 즉시 분석 결과를 제공합니다.
- **보관소 기능:** 관심 있는 프로젝트를 즐겨찾기하여 별도로 필터링하고 관리할 수 있습니다.

---

## 📊 시스템 아키텍처 및 흐름

### ▶️ Sequence Diagram: 자동 수집 및 분석

스케줄러에 의해 백그라운드에서 주기적으로 실행되는 데이터 파이프라인입니다.
![Scheduled Analysis](./1.png)

### ▶️ Sequence Diagram: 사용자 즉시 요청

사용자가 URL을 입력했을 때 비동기 큐를 통해 처리되는 과정입니다.
![On-Demand Analysis](./2.png)

### ▶️ Flow: 갤러리 뷰 데이터 흐름

React 클라이언트와 Spring Boot 서버 간의 데이터 조회 흐름입니다.
![Gallery View](./3.png)

---

## ⚠️ 트러블슈팅 및 성능 고려사항

### 1. 외부 API 지연 및 타임아웃 대응

- **문제:** ComfyUI의 이미지 생성 시간이 5~10초 이상 소요되어, 일반적인 HTTP 요청 시 스레드 차단(Blocking) 발생 가능성.
- **해결:** `Spring WebClient`를 도입하여 Non-blocking 방식으로 API를 호출하고, `@Async` 어노테이션을 적용하여 메인 스레드의 부하를 분산시켰습니다.

### 2. 프롬프트 오염 방지 (Prompt Sanitization)

- **문제:** README에서 추출한 텍스트에 특수문자나 이모지, 코드가 섞여 있어 ComfyUI 워크플로우가 깨지는 현상 발생.
- **해결:** 정규표현식(Regex) 기반의 `sanitize` 로직을 구현하여 순수 텍스트 키워드만 추출하도록 전처리 과정을 강화했습니다.

### 3. GitHub API Rate Limit 제한

- **문제:** 잦은 수집 요청으로 인해 GitHub API의 시간당 요청 제한에 도달하는 문제.
- **해결:** 익명 요청 대신 **Personal Access Token**을 헤더에 주입하여 요청 한도를 5,000회/시간으로 확장하고, 예외 발생 시 스케줄링을 일시 중단하는 로직을 추가했습니다.
