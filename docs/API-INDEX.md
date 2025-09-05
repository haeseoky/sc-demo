# SC-Demo API Documentation Index

## Overview

Spring Boot 데모 애플리케이션의 API 문서 색인입니다. 이 프로젝트는 다양한 Spring 기능을 시연하기 위한 87개의 Java 클래스로 구성되어 있습니다.

## API 엔드포인트 목록

### 🏠 Home & Main
- **HomeController** (`/`)
  - `GET /main` - 메인 페이지

### 🔄 병렬 처리 (Parallel Processing)
- **ParallelController** (`/api/parallel`)
  - `GET /process` - 병렬 처리 성능 테스트
  - 대용량 데이터 처리 및 성능 비교 데모

- **NudgeController** (`/api/nudge`)
  - 알림 메시지 처리 API
  - 병렬 처리를 통한 대량 알림 전송

### 👤 Person 관리 (Sample Domain)
- **PersonController** (`/api/person`)
  - Person 도메인 CRUD 작업
  - JPA, MyBatis, MongoDB 통합 예제

### 🔍 Type System 
- **TypeController** (`/api/type`)
  - Java 타입 시스템 데모
  - 제네릭 및 상속 구조 예제

### 📋 Final 키워드 테스트
- **FinalController** (`/api/final`)
  - Java final 키워드 동작 테스트
  - 불변 객체 및 상수 관리

### 🌐 HTTP 클라이언트
- **HttpClientController** (`/api/http`)
  - WebClient 기반 외부 API 연동
  - Circuit Breaker 패턴 적용

## 서비스 컴포넌트 목록

### 📊 데이터 처리 서비스
- **ParallelService** - CompletableFuture 기반 병렬 처리
- **NudgeService** - 대량 알림 처리 최적화
- **RedisService** - Redis 캐싱 및 데이터 관리

### 🧵 동시성 처리
- **VirtualThreadService** - Java 21 가상 스레드 활용
- **StreamService** - Reactive Streams 구현
- **FutureService** - 비동기 작업 관리

### 🏗️ 도메인 서비스  
- **ShapeService** - 다형성 및 계층 구조 처리
- **RankingService** - Redis 기반 실시간 랭킹
- **PersonCommand/Query** - CQRS 패턴 구현

### ⚙️ 유틸리티 & 설정
- **BannerService** - 애플리케이션 시작 배너
- **JunitTestService** - 테스트 유틸리티
- **StringUtils** - 문자열 처리 헬퍼

## 의존성 주입 예제

### 순환 참조 해결 패턴
- **AService** ↔ **BService** ↔ **CService**
- 생성자 주입 vs 필드 주입 vs 세터 주입 비교

## 설정 클래스 목록

### 🔧 Core Configuration
- **AsyncConfig** - 비동기 처리 설정
- **WebMvcConfig** - Spring MVC 설정  
- **RedisConfig** - Redis 연결 설정
- **MybatisConfig** - MyBatis ORM 설정

### 🛡️ 보안 & 모니터링
- **CustomExceptionHandler** - 전역 예외 처리
- **CustomResponseHandler** - 응답 형식 통일
- **RequestLogFilter** - 요청/응답 로깅

### 🌐 외부 연동
- **WebClientConfig** - HTTP 클라이언트 설정
- **CustomCircuitBreakerConfig** - 회복성 패턴
- **ElasticConfig** - Elasticsearch 설정

### 📚 API 문서화
- **OpenApiConfig** - Swagger/OpenAPI 설정

## 인터셉터 & 필터

### 🔍 모니터링
- **CustomTestInterceptor** - 요청 전후 처리
- **CustomMethodHandler** - 메소드 실행 추적
- **RequestLogFilter** - HTTP 요청 로깅

## 데이터 계층

### 📊 Repository 패턴
- **PersonRepository** - 도메인 저장소 인터페이스
- **PersonJpaRepository** - JPA 구현체
- **TestDataRepository** - 테스트 데이터 관리
- **ElasticRepository** - 검색 엔진 연동

### 📁 엔티티 & DTO
- **PersonEntity** - JPA 엔티티
- **TestData/TestRdbData** - 테스트 데이터 모델
- **Practice** - MyBatis DTO 예제

## API 접근 방법

### Swagger UI
- URL: `http://localhost:8080/swagger-ui/index.html`
- 모든 API 엔드포인트 대화형 문서

### 직접 접근
- Base URL: `http://localhost:8080`
- 각 컨트롤러별 엔드포인트 매핑

## 패키지 구조별 기능

| 패키지 | 주요 기능 | API 엔드포인트 |
|--------|-----------|----------------|
| `parallel` | 병렬 처리 & 성능 최적화 | `/api/parallel/*`, `/api/nudge/*` |
| `sample` | Person 도메인 CRUD | `/api/person/*` |
| `type` | Java 타입 시스템 | `/api/type/*` |
| `finaltest` | Final 키워드 테스트 | `/api/final/*` |
| `http` | 외부 API 연동 | `/api/http/*` |
| `virtual` | 가상 스레드 처리 | 내부 서비스 |
| `redis` | 캐싱 & 세션 관리 | 내부 서비스 |
| `ranking` | 실시간 랭킹 시스템 | 내부 서비스 |
| `hierarchy` | 계층 구조 처리 | 내부 서비스 |
| `stream` | 리액티브 스트림 | 내부 서비스 |

## 관련 문서

- [Project Structure](./PROJECT-STRUCTURE.md) - 전체 프로젝트 구조
- [Feature Documentation](./FEATURES.md) - 패키지별 상세 기능
- [README.md](../README.md) - 프로젝트 개요 및 시작 가이드