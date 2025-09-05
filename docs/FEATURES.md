# SC-Demo Feature Documentation

## 🚀 주요 기능별 상세 가이드

### 🔄 병렬 처리 (Parallel Processing)

#### 📍 위치: `com.ocean.scdemo.parallel`

#### 🎯 목적
- Java CompletableFuture를 활용한 비동기 병렬 처리
- 성능 최적화 및 대용량 데이터 처리 데모
- 동시성 처리 능력 측정 및 모니터링

#### 🔧 주요 컴포넌트

**ParallelService**
- `CompletableFuture` 기반 병렬 작업 처리
- 여러 데이터 소스(JPA, MongoDB) 동시 접근
- 작업 완료 시간 및 처리량 측정

**NudgeService** 
- 대량 알림 메시지 병렬 처리
- 배치 처리를 통한 성능 최적화
- 실시간 처리 상태 모니터링

**RequestCount**
- 동시 요청 수 추적 및 관리
- 부하 테스트 지원
- 메모리 사용량 모니터링

#### 📊 API 엔드포인트
```http
GET /api/parallel/process    # 병렬 처리 성능 테스트
GET /api/nudge/send         # 대량 알림 전송 테스트
```

#### 🧪 테스트 시나리오
- 10,000건 데이터 병렬 처리: ~1.3초
- 5,000,000건 대량 처리: ~248초
- 단일 스레드 대비 성능 향상: 약 10배

---

### 👤 Person 도메인 (Clean Architecture 예제)

#### 📍 위치: `com.ocean.scdemo.sample`

#### 🎯 목적
- 클린 아키텍처 패턴 구현 데모
- DDD(Domain Driven Design) 적용 사례
- 다양한 데이터 접근 방식 통합

#### 🏗️ 아키텍처 계층

**Domain Layer**
```java
Person.java              # 도메인 엔티티
Gender.java              # 도메인 값 객체
PersonRepository.java    # 저장소 인터페이스
```

**Application Layer** 
```java
PersonCommand.java       # 명령 처리 (CQS 패턴)
PersonQuery.java         # 조회 처리 (CQS 패턴)
```

**Infrastructure Layer**
```java
PersonRepositoryImpl.java    # 저장소 구현체
PersonJpaRepository.java     # JPA 저장소
ElasticRepository.java       # 검색 엔진 연동
```

**Presentation Layer**
```java
PersonController.java       # REST API
ResPersonDto.java           # 응답 DTO
```

#### 🔧 기술 스택 통합
- **JPA**: 기본 CRUD 작업
- **MyBatis**: 복잡한 쿼리 처리
- **Elasticsearch**: 전문 검색
- **WebClient**: 외부 API 연동

#### 📊 API 기능
```http
GET    /api/person/{id}     # Person 조회
POST   /api/person          # Person 생성  
PUT    /api/person/{id}     # Person 수정
DELETE /api/person/{id}     # Person 삭제
```

---

### 🧵 가상 스레드 (Virtual Threads)

#### 📍 위치: `com.ocean.scdemo.virtual`

#### 🎯 목적
- Java 21 Project Loom 가상 스레드 활용
- 기존 스레드 모델과 성능 비교
- 높은 동시성 처리 데모

#### 🔧 구현 특징

**VirtualThreadService**
```java
@Service
public class VirtualThreadService {
    // 가상 스레드 executor 사용
    private final ExecutorService virtualExecutor = 
        Executors.newVirtualThreadPerTaskExecutor();
    
    // 성능 비교 메소드
    public CompletionStage<String> processWithVirtualThreads();
    public CompletionStage<String> processWithRegularThreads();
}
```

**VirtualBean**
- Spring 컨텍스트에서 가상 스레드 관리
- Bean 생명주기와 가상 스레드 통합

#### ⚡ 성능 특징
- **메모리 효율성**: 기존 스레드 대비 1/1000 메모리 사용
- **확장성**: 수백만 개의 동시 작업 처리 가능
- **응답성**: I/O 대기 시간 중 CPU 자원 효율적 사용

---

### 🔴 Redis 활용 (Caching & Session)

#### 📍 위치: `com.ocean.scdemo.redis`

#### 🎯 목적
- Redis를 활용한 효율적인 캐싱 전략
- 세션 관리 및 임시 데이터 저장
- 대용량 데이터의 빠른 읽기/쓰기

#### 🔧 주요 컴포넌트

**RedisService**
```java
@Service
public class RedisService {
    // 기본 캐싱 작업
    public void setUserData(String key, UserData data);
    public UserData getUserData(String key);
    
    // 만료 시간 설정
    public void setWithExpiration(String key, Object value, Duration ttl);
}
```

**데이터 모델**
- **UserData**: 사용자 정보 캐싱
- **BannerType**: 배너 데이터 타입 관리  
- **ReadData**: 읽기 전용 데이터 캐싱
- **BigBannerReadData**: 대용량 배너 데이터 처리

#### 🎯 사용 사례
- 사용자 세션 관리
- 자주 조회되는 데이터 캐싱
- 임시 계산 결과 저장
- API 응답 캐싱

---

### 📊 실시간 랭킹 시스템

#### 📍 위치: `com.ocean.scdemo.ranking`

#### 🎯 목적
- Redis Sorted Set을 활용한 실시간 랭킹
- 대용량 사용자 스코어 관리
- 효율적인 순위 계산 및 조회

#### 🔧 RankingService 기능

```java
@Service
public class RankingService {
    // 스코어 업데이트
    public void updateScore(String userId, double score);
    
    // TOP N 조회
    public List<RankingEntry> getTopRanking(int count);
    
    // 특정 사용자 순위 조회
    public Long getUserRank(String userId);
    
    // 범위별 랭킹 조회
    public List<RankingEntry> getRankingRange(int start, int end);
}
```

#### ⚡ 성능 특징
- **실시간 업데이트**: O(log N) 시간 복잡도
- **대용량 처리**: 수백만 사용자 지원
- **메모리 효율성**: Redis 압축 저장

---

### 🏗️ 계층 구조 처리 (Hierarchy)

#### 📍 위치: `com.ocean.scdemo.hierarchy`

#### 🎯 목적
- 객체지향 다형성 구현 데모
- 상속 계층 구조 관리
- 동적 타입 처리 및 팩토리 패턴

#### 🔧 도메인 모델

**Shape 추상 클래스**
```java
public abstract class Shape {
    protected String name;
    public abstract double calculateArea();
    public abstract double calculatePerimeter();
}
```

**구체적 구현체**
```java
Circle.java      # 원형 - π * r²
Rectangle.java   # 사각형 - width * height  
Triangle.java    # 삼각형 - (base * height) / 2
```

**ShapeService**
- 팩토리 패턴을 통한 Shape 객체 생성
- 다형성을 활용한 면적/둘레 계산
- 타입별 특화된 처리 로직

#### 🎯 학습 목표
- 추상화와 캡슐화
- 다형성 활용 패턴
- 팩토리 메소드 패턴
- 전략 패턴 응용

---

### 🚀 스트림 API & 리액티브 (Stream & Reactive)

#### 📍 위치: `com.ocean.scdemo.stream`

#### 🎯 목적
- Java 8+ 스트림 API 활용
- Reactive Streams 구현
- 함수형 프로그래밍 패러다임

#### 🔧 주요 컴포넌트

**StreamService**
```java
@Service  
public class StreamService {
    // 함수형 데이터 처리
    public List<T> processWithStreams(List<T> data);
    
    // 병렬 스트림 활용
    public Result parallelProcessing(Stream<T> stream);
}
```

**리액티브 구현**
```java
FluxService.java        # Project Reactor Flux 활용
CustomPublisher.java    # 커스텀 발행자 구현
CustomSubscriber.java   # 커스텀 구독자 구현
PubSubOnService.java    # 발행-구독 패턴
```

**비동기 처리**
```java
FutureService.java      # CompletableFuture 활용
IntervalEx.java         # 주기적 실행 패턴
```

#### 🎯 활용 사례
- 대량 데이터 변환 및 필터링
- 비동기 이벤트 스트림 처리
- 백프레셔 제어가 있는 데이터 파이프라인

---

### 🔗 의존성 주입 패턴 (Dependency Injection)

#### 📍 위치: `com.ocean.scdemo.inject`

#### 🎯 목적
- Spring 의존성 주입 방식 비교
- 순환 참조 문제 해결 데모
- 다양한 주입 패턴 학습

#### 🔧 구현 예제

**순환 참조 시나리오**
```java
@Service
public class AService {
    private final BService bService;
    // A → B → C → A 순환 구조
}

@Service  
public class BService {
    private final CService cService;
}

@Service
public class CService {
    private final AService aService;
}
```

#### 🛠️ 해결 방법
1. **생성자 주입**: `@Lazy` 어노테이션 활용
2. **세터 주입**: 지연 로딩으로 해결
3. **@PostConstruct**: 초기화 후 의존성 설정
4. **ApplicationContext**: 런타임 의존성 조회

---

### 🌐 HTTP 클라이언트 & Circuit Breaker

#### 📍 위치: `com.ocean.scdemo.http`

#### 🎯 목적
- Spring WebClient 활용 외부 API 연동
- Circuit Breaker 패턴 구현
- 장애 격리 및 회복성 확보

#### 🔧 주요 구성요소

**HttpClientController**
```java
@RestController
public class HttpClientController {
    @GetMapping("/api/http/external")
    public Mono<String> callExternalApi() {
        return webClient.get()
            .uri("/api/endpoint")
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofSeconds(5))
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)));
    }
}
```

**Circuit Breaker 설정**
```java
@Configuration
public class CustomCircuitBreakerConfig {
    @Bean
    public CircuitBreaker circuitBreaker() {
        return CircuitBreaker.ofDefaults("httpClient")
            .withFailureRateThreshold(50)
            .withWaitDurationInOpenState(Duration.ofMillis(1000))
            .withSlidingWindowSize(2);
    }
}
```

#### ⚡ 장애 대응 전략
- **타임아웃**: 5초 제한
- **재시도**: 지수 백오프 3회
- **Circuit Breaker**: 실패율 50% 시 차단
- **Fallback**: 기본 응답 반환

---

### 🧪 테스트 전략 (Testing Strategy)

#### 📍 위치: `com.ocean.scdemo.junittest`

#### 🎯 목적
- 효과적인 단위 테스트 작성
- 테스트 유틸리티 제공
- 테스트 가능한 코드 설계

#### 🔧 테스트 구성요소

**JunitTestService**
- 테스트 데이터 생성 유틸리티
- Mock 객체 관리
- 테스트 시나리오 템플릿

**JunitTestProvider**
- 테스트용 데이터 공급자
- 다양한 테스트 케이스 제공
- 경계값 테스트 데이터

#### 📊 테스트 유형
- **단위 테스트**: 개별 컴포넌트 검증
- **통합 테스트**: 컴포넌트 간 상호작용
- **성능 테스트**: 병렬 처리 성능 측정
- **E2E 테스트**: 전체 워크플로우 검증

---

### ⚙️ 설정 관리 (Configuration Management)

#### 📍 위치: `com.ocean.scdemo.config`

#### 🎯 목적  
- Spring Boot 자동 설정 커스터마이징
- 환경별 설정 관리
- Bean 생명주기 제어

#### 🔧 주요 설정 클래스

**AsyncConfig**
```java
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean("taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        return executor;
    }
}
```

**WebMvcConfig**
- 인터셉터 등록
- CORS 설정
- 정적 리소스 매핑

**OpenApiConfig**
- Swagger UI 설정  
- API 문서화 커스터마이징
- 보안 스키마 정의

#### 🌍 환경별 프로파일
- **local**: 로컬 개발 환경
- **dev**: 개발 서버 환경  
- **stage**: 스테이징 환경
- **prod**: 프로덕션 환경

---

## 🔄 교차 기능 (Cross-Cutting Concerns)

### 🔍 인터셉터 & 필터
- **CustomTestInterceptor**: 요청/응답 로깅
- **RequestLogFilter**: HTTP 요청 추적
- **CustomMethodHandler**: 메소드 실행 모니터링

### 🛡️ 예외 처리
- **CustomExceptionHandler**: 전역 예외 처리
- **CustomResponseHandler**: 일관된 응답 형식
- **ExceptionResponse**: 표준 오류 응답

### 📊 모니터링 & 관측성
- Actuator 엔드포인트 활성화
- 성능 메트릭 수집
- 애플리케이션 상태 모니터링

---

## 🚀 실행 및 테스트 방법

### 💾 데이터베이스 준비
```bash
# MariaDB 실행 (포트: 3306)
# MongoDB 실행 (포트: 27017)  
# Redis 실행 (포트: 6379)
```

### 🏃 애플리케이션 실행
```bash
./gradlew bootRun
```

### 📊 API 테스트
- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **메인 페이지**: http://localhost:8080/main
- **헬스 체크**: http://localhost:8080/actuator/health

### 🧪 성능 테스트
```bash
# 병렬 처리 테스트
curl http://localhost:8080/api/parallel/process

# 가상 스레드 테스트  
curl http://localhost:8080/api/virtual/benchmark
```

---

## 📚 학습 포인트

### 🎯 아키텍처 패턴
- **클린 아키텍처**: 계층 분리 및 의존성 역전
- **CQRS**: 명령과 조회 분리
- **이벤트 소싱**: 도메인 이벤트 활용

### 🔧 기술적 패턴  
- **팩토리 패턴**: 객체 생성 추상화
- **전략 패턴**: 알고리즘 교체 가능성
- **옵서버 패턴**: 이벤트 기반 아키텍처

### ⚡ 성능 최적화
- **병렬 처리**: CompletableFuture & 가상 스레드
- **캐싱 전략**: Redis 활용 다단계 캐싱
- **비동기 처리**: 논블로킹 I/O & 리액티브

---

## 📖 관련 문서

- [API Documentation Index](./API-INDEX.md)
- [Project Structure](./PROJECT-STRUCTURE.md)  
- [Main README](../README.md)