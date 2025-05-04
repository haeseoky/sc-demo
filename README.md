# SC-Demo 프로젝트

Spring Boot 기반의 데모 애플리케이션입니다. 다양한 Spring 기능을 시험하고 학습하기 위한 샘플 프로젝트입니다.

## 기술 스택

- **언어**: Java 21
- **프레임워크**: Spring Boot 3.4.1
- **빌드 도구**: Gradle
- **데이터베이스**:
  - MariaDB (관계형 데이터베이스)
  - MongoDB (NoSQL 데이터베이스)
  - Redis (인메모리 데이터베이스)
- **데이터 접근**: 
  - Spring Data JPA
  - MyBatis
  - Spring Data JDBC
- **API 문서화**: SpringDoc OpenAPI (Swagger UI)
- **템플릿 엔진**: Thymeleaf
- **클라이언트**: Spring WebFlux (리액티브 웹 클라이언트)
- **회복성 패턴**: Resilience4j (서킷 브레이커)
- **검색 엔진**: Elasticsearch

## 주요 기능

이 프로젝트는 다음과 같은 다양한 Spring 기능의 데모를 포함하고 있습니다:

### 병렬 처리 (parallel 패키지)
- **ParallelService**: Java의 CompletableFuture를 활용한 비동기 병렬 처리 구현
- **NudgeService**: 대량의 알림 메시지를 효율적으로 처리하는 병렬 처리 예제
- **ParallelController**: REST API를 통해 병렬 처리 기능 노출
- **RequestCount**: 동시 요청 수 관리 및 모니터링 기능
- **병렬 데이터 접근**: 여러 데이터 소스(JPA, MongoDB)에 대한 동시 접근 최적화

### Redis 활용 (redis 패키지)
- **RedisService**: Redis를 활용한 캐싱 및 데이터 저장 기능
- **UserData**: 사용자 정보의 효율적인 관리를 위한 Redis 활용
- **BannerType**: 배너 데이터의 유형 관리 및 캐싱 전략
- **ReadData/BigBannerReadData**: 대용량 데이터의 효율적인 읽기 처리

### 가상 스레드 활용 (virtual 패키지)
- **VirtualThreadService**: Java 21의 가상 스레드(Project Loom) 기능 활용
- **성능 비교**: 기존 스레드 모델과 가상 스레드의 성능 비교 데모
- **VirtualBean**: 스프링 컨텍스트에서 가상 스레드 관리 방법

### MyBatis 활용 (mybatis 패키지)
- **DTO와 매퍼**: 복잡한 SQL 쿼리를 효율적으로 관리하는 MyBatis 매퍼 구성
- **동적 SQL**: 조건에 따라 변경되는 동적 SQL 쿼리 작성 방법
- **XML 설정 vs 어노테이션**: 다양한 MyBatis 설정 방식 비교

### 계층 구조 처리 (hierarchy 패키지)
- 트리 구조의 데이터 관리 및 처리
- 계층형 데이터에 대한 재귀적 처리 알고리즘
- 계층 구조의 효율적인 데이터베이스 매핑 방법

### 랭킹 시스템 (ranking 패키지)
- 효율적인 순위 계산 알고리즘 구현
- Redis를 활용한 실시간 랭킹 데이터 관리
- 대용량 데이터에서의 성능 최적화 기법

### 의존성 주입 예제 (inject 패키지)
- 다양한 의존성 주입 방식 (생성자, 필드, 세터 주입)
- 순환 참조 문제 해결 방법
- 조건부 빈 구성 및 프로파일 기반 주입

### Java 스트림 API 활용 (stream 패키지)
- 함수형 프로그래밍 패러다임을 활용한 데이터 처리
- 복잡한 비즈니스 로직에 스트림 API 적용 사례
- 성능 최적화를 위한 병렬 스트림 활용

### 커스텀 어노테이션 (annotation 패키지)
- 메타데이터 기반 프로그래밍 구현
- AOP와 연동한 커스텀 어노테이션 활용
- 실행 시간 측정, 로깅, 권한 체크 등의 횡단 관심사 처리

### HTTP 클라이언트 (http 패키지)
- WebClient를 활용한 비동기 HTTP 클라이언트 구현
- 외부 API 연동 및 오류 처리 전략
- Circuit Breaker 패턴을 통한 장애 대응

### 예외 처리 패턴 (exception 패키지)
- 전역 예외 처리기 구현
- 비즈니스 예외와 시스템 예외의 분리
- 예외 발생 시 일관된 응답 형식 유지

### 인터셉터 활용 (interceptor 패키지)
- 요청/응답 처리 전후의 공통 로직 구현
- 사용자 인증 및 권한 체크
- 성능 모니터링 및 로깅

## 시작하기

### 필수 조건

- JDK 21
- Gradle
- MariaDB
- MongoDB
- Redis
- Docker (선택 사항)

### 로컬 환경 설정

1. 레포지토리를 클론합니다:
   ```bash
   git clone [repository-url]
   cd sc-demo
   ```

2. 데이터베이스 설정:
   - MariaDB 실행 (포트: 3306)
   - MongoDB 실행 (포트: 27017)
   - Redis 실행 (포트: 6379)
   
3. 애플리케이션을 빌드하고 실행합니다:
   ```bash
   ./gradlew bootRun
   ```

4. 애플리케이션에 접근합니다:
   - 기본 URL: http://localhost:8080
   - Swagger UI: http://localhost:8080/swagger-ui/index.html
   - 메인 페이지: http://localhost:8080/main

## 데모 시나리오

프로젝트에는 다음과 같은 데모 시나리오가 포함되어 있습니다:

1. **대용량 데이터 병렬 처리**:
   - `/api/parallel/process` 엔드포인트를 통해 병렬 처리 성능 테스트
   - 초당 처리량 및 응답 시간 측정

2. **실시간 랭킹 시스템**:
   - Redis 기반 실시간 랭킹 업데이트
   - 대용량 사용자의 스코어 갱신 및 조회

3. **계층형 데이터 관리**:
   - 조직 구조와 같은 계층형 데이터의 효율적인 CRUD 작업
   - 재귀적 트리 구조 탐색 알고리즘

4. **가상 스레드 성능 테스트**:
   - 기존 스레드 풀 vs 가상 스레드 성능 비교
   - 동시성 처리 능력 및 리소스 사용량 모니터링

## 프로젝트 구조

프로젝트는 주요 기능별로 패키지가 구분되어 있습니다:

```
src/main/java/com/ocean/scdemo/
├── annotation    - 커스텀 어노테이션 정의 및 활용
├── banner        - 애플리케이션 시작 시 배너 관련
├── config        - 애플리케이션 설정 클래스
├── controller    - API 컨트롤러
├── exception     - 예외 처리 관련
├── finaltest     - final 키워드 테스트
├── hierarchy     - 계층 구조 처리
├── http          - HTTP 클라이언트 예제
├── inject        - 의존성 주입 예제
├── interceptor   - 인터셉터 활용
├── junittest     - 단위 테스트 관련 
├── mybatis       - MyBatis 사용 예제
├── parallel      - 병렬 프로그래밍 예제
├── ranking       - 랭킹 시스템 구현
├── redis         - Redis 사용 예제
├── sample        - 기타 예제 코드
├── stream        - Java 스트림 API 활용
├── type          - 타입 관련 테스트
├── util          - 유틸리티 클래스
└── virtual       - 가상 스레드 활용
```

## 환경 설정

애플리케이션은 다음과 같은 환경 프로파일을 지원합니다:

- **local**: 로컬 개발 환경 (기본값)
- **dev**: 개발 서버 환경
- **stage**: 스테이징 환경
- **prod**: 프로덕션 환경

각 환경에 따른 설정은 `application.yml` 파일에서 관리됩니다.

## API 문서

API 문서는 Swagger UI를 통해 제공됩니다. 애플리케이션 실행 후 다음 URL에서 확인할 수 있습니다:
http://localhost:8080/swagger-ui/index.html

## 테스트

테스트를 실행하려면 다음 명령을 사용합니다:

```bash
./gradlew test
```

테스트 결과는 JSON 형식으로 변환되어 `test-results.json` 파일에 저장됩니다. 이 프로젝트는 다음과 같은 테스트 전략을 사용합니다:

- **단위 테스트**: 개별 컴포넌트의 기능 검증
- **통합 테스트**: 여러 컴포넌트 간의 상호작용 검증
- **성능 테스트**: 병렬 처리 및 가상 스레드의 성능 측정

## 라이센스

[라이센스 정보를 입력하세요]

## 기여하기

[기여 방법에 대한 정보를 입력하세요]

## 연락처

[연락처 정보를 입력하세요]