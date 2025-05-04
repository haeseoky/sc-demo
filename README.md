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

- 다양한 유형의 데이터베이스 연결 및 사용
- 병렬 처리 (parallel 패키지)
- 계층 구조 처리 (hierarchy 패키지)
- 랭킹 시스템 (ranking 패키지)
- MyBatis 활용 사례 (mybatis 패키지)
- 의존성 주입 예제 (inject 패키지)
- Redis 캐시 활용 (redis 패키지)
- Java 스트림 API 활용 (stream 패키지)
- 커스텀 어노테이션 (annotation 패키지)
- HTTP 클라이언트 (http 패키지)
- 가상 스레드 활용 (virtual 패키지)
- 예외 처리 패턴 (exception 패키지)
- 인터셉터 활용 (interceptor 패키지)

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

테스트 결과는 JSON 형식으로 변환되어 `test-results.json` 파일에 저장됩니다.

## 라이센스

[라이센스 정보를 입력하세요]

## 기여하기

[기여 방법에 대한 정보를 입력하세요]

## 연락처

[연락처 정보를 입력하세요]