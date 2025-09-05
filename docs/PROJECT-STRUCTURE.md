# SC-Demo Project Structure

## 📁 전체 프로젝트 구조

```
sc-demo/
├── 📄 README.md                          # 프로젝트 개요 및 시작 가이드
├── 📄 HELP.md                            # 추가 도움말 및 Kafka 테스트 정보
├── 📄 Result.md                          # 성능 테스트 결과
├── 📄 build.gradle                       # Gradle 빌드 설정
├── 📁 docs/                              # 프로젝트 문서
│   ├── API-INDEX.md                      # API 문서 색인
│   ├── PROJECT-STRUCTURE.md              # 이 문서
│   └── FEATURES.md                       # 패키지별 기능 문서
├── 📁 src/main/java/com/ocean/
│   ├── 📁 controller/                    # 추가 컨트롤러
│   └── 📁 scdemo/                        # 메인 애플리케이션 패키지
│       ├── 📄 ScDemoApplication.java     # Spring Boot 메인 클래스
│       ├── 📁 annotation/                # 커스텀 어노테이션
│       ├── 📁 banner/                    # 시작 배너 관련
│       ├── 📁 config/                    # 설정 클래스들
│       ├── 📁 exception/                 # 예외 처리
│       ├── 📁 finaltest/                 # Final 키워드 테스트
│       ├── 📁 hierarchy/                 # 계층 구조 처리
│       ├── 📁 http/                      # HTTP 클라이언트
│       ├── 📁 inject/                    # 의존성 주입 예제
│       ├── 📁 interceptor/               # 인터셉터
│       ├── 📁 junittest/                 # 단위 테스트
│       ├── 📁 mybatis/                   # MyBatis 사용 예제
│       ├── 📁 parallel/                  # 병렬 프로그래밍
│       ├── 📁 ranking/                   # 랭킹 시스템
│       ├── 📁 redis/                     # Redis 사용 예제
│       ├── 📁 sample/                    # Person 도메인 예제
│       ├── 📁 stream/                    # 스트림 API 활용
│       ├── 📁 type/                      # 타입 시스템 테스트
│       ├── 📁 util/                      # 유틸리티 클래스
│       └── 📁 virtual/                   # 가상 스레드 활용
└── 📁 src/main/resources/
    ├── 📄 application.yml                # 메인 설정 파일
    ├── 📄 application-local.yml          # 로컬 환경 설정
    └── 📄 banner.txt                     # 시작 배너 텍스트
```

## 🏗️ 아키텍처 계층

### 1. Presentation Layer (표현 계층)
```
📁 controller/
├── HomeController.java                   # 메인 페이지 
📁 */presentation/
├── *Controller.java                      # 각 도메인별 컨트롤러
├── model/                               # DTO 및 요청/응답 모델
└── listener/                            # 이벤트 리스너
```

### 2. Application Layer (애플리케이션 계층)
```
📁 */application/
├── *Command.java                        # 명령 처리 서비스
├── *Query.java                          # 조회 서비스
└── *Service.java                        # 비즈니스 로직
```

### 3. Domain Layer (도메인 계층)
```
📁 */domain/
├── *.java                               # 도메인 엔티티
├── repository/                          # 도메인 저장소 인터페이스
└── [enum/value objects]                 # 도메인 값 객체
```

### 4. Infrastructure Layer (인프라 계층)
```
📁 */infrastructure/
├── *RepositoryImpl.java                 # 저장소 구현체
├── jpa/                                 # JPA 관련
├── model/                               # 인프라 모델
│   ├── entity/                          # JPA 엔티티
│   └── response/                        # 외부 응답 모델
└── [webclient/elastic/etc]              # 외부 연동
```

## 📦 패키지별 상세 구조

### 🔧 Config Package
Spring Boot 설정 및 Bean 구성
```
📁 config/
├── AsyncConfig.java                     # 비동기 처리 설정
├── CustomCircuitBreakerConfig.java      # Circuit Breaker 설정
├── CustomCommandLineRunner.java         # 애플리케이션 시작 후 실행
├── CustomExceptionHandler.java          # 전역 예외 처리
├── CustomResponseHandler.java           # 응답 형식 통일
├── ElasticConfig.java                   # Elasticsearch 설정
├── MybatisConfig.java                   # MyBatis 설정
├── OpenApiConfig.java                   # Swagger/OpenAPI 설정
├── RedisConfig.java                     # Redis 설정
├── WebClientConfig.java                 # HTTP 클라이언트 설정
├── WebMvcConfig.java                    # Spring MVC 설정
├── circuitbreaker/                      # Circuit Breaker 구현
├── filter/                              # HTTP 필터
└── model/                               # 공통 응답 모델
```

### 🔄 Parallel Package
병렬 처리 및 성능 최적화
```
📁 parallel/
├── ParallelController.java              # 병렬 처리 API
├── ParallelService.java                 # 병렬 처리 로직
├── NudgeController.java                 # 알림 처리 API
├── NudgeService.java                    # 대량 알림 처리
├── RequestCount.java                    # 동시 요청 수 모니터링
├── TestData.java                        # 테스트 데이터 모델
├── TestRdbData.java                     # RDB 테스트 데이터
├── TestDataRepository.java              # MongoDB 저장소
└── TestRdbDataJpaRepository.java        # JPA 저장소
```

### 👤 Sample Package (Clean Architecture 예제)
Person 도메인을 통한 클린 아키텍처 구현
```
📁 sample/
├── 📁 domain/
│   ├── Person.java                      # 도메인 엔티티
│   ├── Gender.java                      # 도메인 열거형
│   └── repository/
│       └── PersonRepository.java        # 저장소 인터페이스
├── 📁 application/
│   ├── PersonCommand.java               # 명령 처리
│   └── PersonQuery.java                 # 조회 처리
├── 📁 presentation/
│   ├── PersonController.java            # REST API
│   ├── SampleListener.java              # 이벤트 리스너
│   └── model/
│       └── ResPersonDto.java            # 응답 DTO
└── 📁 infrastructure/
    ├── PersonRepositoryImpl.java        # 저장소 구현
    ├── LocalTestWebClient.java          # 외부 API 클라이언트
    ├── LogTester.java                   # 로깅 테스트
    ├── ElasticRepository.java           # Elasticsearch 연동
    ├── jpa/
    │   └── PersonJpaRepository.java     # JPA 저장소
    └── model/
        ├── entity/                      # JPA 엔티티
        └── response/                    # 외부 응답
```

### 🧵 Virtual Package
Java 21 가상 스레드 활용
```
📁 virtual/
├── VirtualThreadService.java            # 가상 스레드 서비스
└── VirtualBean.java                     # 스프링 빈 관리
```

### 🏗️ Hierarchy Package  
계층 구조 및 다형성 처리
```
📁 hierarchy/
├── 📁 domain/
│   ├── Shape.java                       # 추상 기본 클래스
│   ├── Circle.java                      # 원형 구현
│   ├── Rectangle.java                   # 사각형 구현
│   └── Triangle.java                    # 삼각형 구현
└── 📁 application/
    └── ShapeService.java                # 도형 처리 서비스
```

### 🚀 Stream Package
리액티브 스트림 및 함수형 프로그래밍
```
📁 stream/
├── StreamService.java                   # 스트림 API 활용
├── FluxService.java                     # Reactive Streams
├── FutureService.java                   # CompletableFuture
├── IntervalEx.java                      # 주기적 실행
├── PubSubOnService.java                 # 발행-구독 패턴
├── CustomPublisher.java                 # 커스텀 발행자
└── CustomSubstriber.java                # 커스텀 구독자
```

### 🔴 Redis Package
Redis 활용 캐싱 및 세션 관리
```
📁 redis/
├── RedisService.java                    # Redis 서비스
├── UserData.java                        # 사용자 데이터
├── BannerType.java                      # 배너 타입 관리
├── ReadData.java                        # 읽기 데이터
└── BigBannerReadData.java               # 대용량 배너 데이터
```

### 📊 Ranking Package
실시간 랭킹 시스템
```
📁 ranking/
└── RankingService.java                  # Redis 기반 랭킹
```

### 🔗 Inject Package
의존성 주입 패턴 예제
```
📁 inject/
├── AService.java                        # 서비스 A (순환 참조)
├── BService.java                        # 서비스 B (순환 참조)
└── CService.java                        # 서비스 C (순환 참조)
```

## 🛠️ 기술 스택 매핑

### 데이터 저장소 연동
| 기술 | 패키지 | 용도 |
|------|--------|------|
| **JPA** | `sample.infrastructure.jpa` | 관계형 데이터 CRUD |
| **MyBatis** | `mybatis` | 복잡한 SQL 쿼리 |
| **MongoDB** | `parallel` | NoSQL 테스트 데이터 |
| **Redis** | `redis`, `ranking` | 캐싱, 세션, 랭킹 |
| **Elasticsearch** | `sample.infrastructure` | 검색 엔진 |

### 동시성 처리
| 기술 | 패키지 | 용도 |
|------|--------|------|
| **CompletableFuture** | `parallel` | 비동기 병렬 처리 |
| **Virtual Threads** | `virtual` | 경량 스레드 처리 |
| **Reactive Streams** | `stream` | 리액티브 프로그래밍 |
| **@Async** | `config.AsyncConfig` | Spring 비동기 |

### 외부 연동
| 기술 | 패키지 | 용도 |
|------|--------|------|
| **WebClient** | `http`, `sample.infrastructure` | HTTP 클라이언트 |
| **Circuit Breaker** | `config.circuitbreaker` | 장애 격리 |
| **Resilience4j** | `config.CustomCircuitBreakerConfig` | 회복성 패턴 |

## 📋 설정 파일 구조

### application.yml
```yaml
spring:
  profiles:
    active: local                        # 기본 프로파일
  application:
    name: sc-demo
  datasource:                            # MariaDB 설정
  data:
    mongodb:                             # MongoDB 설정
    redis:                              # Redis 설정
  jpa:                                  # JPA 설정
  elasticsearch:                        # Elasticsearch 설정

management:                             # Actuator 설정
springdoc:                              # OpenAPI 설정
```

### application-local.yml
```yaml
# 로컬 개발 환경 전용 설정
logging:
  level:
    com.ocean.scdemo: DEBUG
```

## 🧪 테스트 구조

### 단위 테스트
- `junittest/` - JUnit 테스트 유틸리티
- 각 패키지별 `*Test.java` 파일들

### 통합 테스트  
- Spring Boot Test 활용
- 실제 데이터베이스 연동 테스트

### 성능 테스트
- `Result.md` - 병렬 처리 성능 측정 결과
- 가상 스레드 vs 일반 스레드 벤치마크

## 🚀 실행 흐름

1. **ScDemoApplication.java** - 애플리케이션 시작점
2. **CustomCommandLineRunner** - 초기화 작업 실행  
3. **Banner.java** - 시작 배너 출력
4. **Config 클래스들** - Spring 컨텍스트 설정
5. **Controller들** - HTTP 요청 처리 준비

## 📚 관련 문서

- [API Documentation Index](./API-INDEX.md)
- [Feature Documentation](./FEATURES.md)
- [Main README](../README.md)