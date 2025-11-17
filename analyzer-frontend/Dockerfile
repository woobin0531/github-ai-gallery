# 1. 빌드 환경 (Node.js 20 사용)
FROM node:20-alpine AS build
WORKDIR /app

# package.json 파일 먼저 복사 (의존성 캐싱)
COPY package.json .
COPY package-lock.json .

# 의존성 설치
RUN npm install

# 전체 소스 코드 복사
COPY . .

# React 앱 빌드 (Vite 사용)
RUN npm run build

# 2. 서빙 환경 (Nginx 사용)
FROM nginx:1.27-alpine

# Nginx 기본 설정 파일 삭제
RUN rm /etc/nginx/conf.d/default.conf

# 우리가 만든 Nginx 설정 파일 복사
COPY nginx.conf /etc/nginx/conf.d/

# 1단계(build)에서 빌드된 결과물(dist 폴더)을 Nginx의 웹서버 폴더로 복사
COPY --from=build /app/dist /usr/share/nginx/html

# 5173 포트 개방
EXPOSE 5173

# Nginx 실행
CMD ["nginx", "-g", "daemon off;"]