# 1. Java 17 빌드 환경
FROM eclipse-temurin:17-jdk-focal AS build

WORKDIR /workspace/app

# Gradle 래퍼 파일 복사
COPY gradlew .
COPY gradle ./gradle

# build.gradle 파일 복사 (의존성 캐싱을 위해)
COPY build.gradle .
COPY settings.gradle .

# 의존성 먼저 다운로드 (RUN ... || true 는 오류가 나도 일단 넘어가게 해줌)
RUN ./gradlew build || true 

# 전체 소스 코드 복사
COPY src ./src

# 애플리케이션 빌드 (bootJar 태스크 실행)
RUN ./gradlew bootJar

# 2. 최종 실행 환경 (경량 이미지 사용)
FROM eclipse-temurin:17-jre-focal
VOLUME /tmp
# 빌드 환경(build)에서 만들어진 .jar 파일을 app.jar로 복사
COPY --from=build /workspace/app/build/libs/*.jar app.jar
# 컨테이너가 실행될 때 이 명령어를 실행
ENTRYPOINT ["java","-jar","/app.jar"]