# SC-Demo 프로젝트 문서

이 디렉토리는 SC-Demo 프로젝트의 종합 문서를 포함합니다.

## 📚 문서 목록

### 🎯 [API 문서 색인](./API-INDEX.md)
- 모든 REST API 엔드포인트 목록
- 컨트롤러별 API 기능 설명
- 서비스 컴포넌트 매핑
- Swagger UI 접근 정보

### 🏗️ [프로젝트 구조](./PROJECT-STRUCTURE.md)
- 전체 디렉토리 구조
- 패키지별 역할 및 책임
- 아키텍처 계층 구조
- 기술 스택 매핑

### 🚀 [기능 상세 가이드](./FEATURES.md)
- 패키지별 핵심 기능 설명
- 구현 패턴 및 예제
- 성능 특성 및 테스트 방법
- 학습 포인트 및 활용 사례

## 🎯 빠른 시작

### 1. 프로젝트 이해
1. [README.md](../README.md) - 프로젝트 전체 개요
2. [PROJECT-STRUCTURE.md](./PROJECT-STRUCTURE.md) - 구조 파악
3. [FEATURES.md](./FEATURES.md) - 주요 기능 학습

### 2. API 사용
1. [API-INDEX.md](./API-INDEX.md) - API 목록 확인
2. http://localhost:8080/swagger-ui/index.html - 대화형 API 문서

### 3. 코드 탐색
```
src/main/java/com/ocean/scdemo/
├── parallel/     # 병렬 처리 예제부터 시작 추천
├── sample/       # 클린 아키텍처 패턴 학습
├── virtual/      # Java 21 가상 스레드 체험
└── config/       # Spring Boot 설정 이해
```

## 📊 주요 데모 시나리오

### 🔄 병렬 처리 성능 테스트
```bash
curl http://localhost:8080/api/parallel/process
```

### 👤 Person 도메인 CRUD
```bash
# 조회
curl http://localhost:8080/api/person/1

# 생성
curl -X POST http://localhost:8080/api/person \
  -H "Content-Type: application/json" \
  -d '{"name":"홍길동","age":30,"gender":"MALE"}'
```

### 🧵 가상 스레드 벤치마크
```bash
curl http://localhost:8080/api/virtual/benchmark
```

## 🛠️ 개발 환경 설정

### 필수 준비사항
- JDK 21
- MariaDB (포트: 3306)
- MongoDB (포트: 27017)
- Redis (포트: 6379)

### 실행 명령
```bash
# 의존성 설치 및 빌드
./gradlew build

# 애플리케이션 실행
./gradlew bootRun

# 테스트 실행
./gradlew test
```

## 📈 성능 측정 결과

성능 테스트 결과는 [Result.md](../Result.md)에서 확인할 수 있습니다:

- **10,000건 병렬 처리**: ~1.3초
- **단일 스레드 대비 성능**: 약 10배 향상
- **가상 스레드 메모리 효율성**: 기존 대비 1/1000

## 🎓 학습 추천 경로

### 초급 (Spring Boot 기초)
1. **config/** - Spring Boot 설정 이해
2. **sample/** - 기본 CRUD 작업
3. **inject/** - 의존성 주입 패턴

### 중급 (성능 최적화)
1. **parallel/** - 병렬 처리 기법
2. **redis/** - 캐싱 전략
3. **stream/** - 함수형 프로그래밍

### 고급 (최신 기술 활용)
1. **virtual/** - Java 21 가상 스레드
2. **hierarchy/** - 고급 객체지향 설계
3. **http/** - 외부 연동 & 장애 대응

## 🔗 외부 리소스

- [Spring Boot 공식 문서](https://docs.spring.io/spring-boot/)
- [Java 21 Virtual Threads](https://openjdk.org/jeps/444)
- [Project Reactor](https://projectreactor.io/)
- [Redis Documentation](https://redis.io/documentation)

## 📞 문의 및 기여

프로젝트 관련 문의사항이나 개선 제안은 이슈 또는 풀 리퀘스트로 남겨주세요.

---

*이 문서는 `/sc:index` 명령을 통해 자동 생성되었습니다.*