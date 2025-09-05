# 🚀 고성능 다단계 캐시 시스템

## 📋 개요

Redis와 Caffeine을 활용한 2단계 캐시 시스템으로 최고의 데이터 조회 성능을 제공합니다.

### 🏗️ 아키텍처

```
요청 → L1 Cache (Caffeine) → L2 Cache (Redis) → Database
      ↑ 초고속 (ns)        ↑ 고속 (ms)        ↑ 느림 (ms)
      ↑ 로컬 메모리        ↑ 네트워크           ↑ 디스크 I/O
```

## 🔧 주요 컴포넌트

### 1. MultiLevelCacheConfig
- **L1 캐시**: Caffeine (로컬 인메모리)
- **L2 캐시**: Redis (분산 캐시)
- **통합 관리**: 자동 failover 및 캐시 워밍

### 2. HighPerformanceCacheService
- **@Cacheable**: 자동 캐시 조회
- **@CachePut**: 캐시 업데이트  
- **@CacheEvict**: 캐시 무효화
- **비동기 처리**: CompletableFuture 활용

### 3. CacheMetricsService
- **실시간 모니터링**: 히트율, 응답시간 추적
- **성능 분석**: 캐시 효율성 분석
- **알람 시스템**: 성능 저하 시 알림

## ⚡ 성능 특징

### L1 Cache (Caffeine)
- **응답시간**: < 1ms (나노초 단위)
- **용량**: 메모리 기반 (10K-50K entries)
- **TTL**: 1-10분 (짧은 주기)
- **특징**: CPU 캐시 친화적, 제로 가비지

### L2 Cache (Redis)  
- **응답시간**: 1-5ms
- **용량**: 대용량 (GB 단위)
- **TTL**: 10분-24시간 (긴 주기)
- **특징**: 분산 환경 공유, 영속성

### 성능 비교
```
직접 DB 조회:     100-500ms
Redis만 사용:     1-5ms    (20-100x 향상)
Caffeine만 사용:  <1ms     (100-500x 향상)  
다단계 캐시:       <1ms     (최상의 히트율)
```

## 📊 API 엔드포인트

### 데이터 조회 API
```http
GET /api/cache/users/{userId}           # 사용자 조회
GET /api/cache/products/{productId}     # 상품 조회  
GET /api/cache/hotdata/{dataKey}        # 실시간 데이터
POST /api/cache/users/batch             # 배치 조회
```

### 성능 테스트 API
```http
POST /api/cache/performance-test        # 부하 테스트
POST /api/cache/warmup                  # 캐시 예열
DELETE /api/cache/users/cache/clear     # 캐시 클리어
```

### 모니터링 API  
```http
GET /api/cache/metrics/report           # 전체 성능 리포트
GET /api/cache/metrics/caffeine         # L1 통계
GET /api/cache/metrics/redis            # L2 통계
GET /api/cache/metrics/analysis/{name}  # 상세 분석
```

## 🎯 사용 예제

### 1. 기본 캐시 조회
```java
@Service
public class UserService {
    
    @Cacheable(value = "users", key = "#userId")
    public UserData getUser(String userId) {
        // DB 조회 (캐시 미스시만 실행)
        return userRepository.findById(userId);
    }
}
```

### 2. 조건부 캐싱
```java
@Cacheable(value = "products", 
           key = "#productId",
           condition = "#productId != null",
           unless = "#result.price < 100")
public ProductData getProduct(String productId) {
    return productRepository.findById(productId);
}
```

### 3. 배치 조회
```java
public Map<String, UserData> getBatchUsers(List<String> userIds) {
    return cacheService.getBatch(userIds, "users", 
        missedKeys -> userRepository.findByIdIn(missedKeys));
}
```

## 📈 성능 최적화 전략

### 1. 캐시 계층별 최적화
- **L1**: 자주 접근하는 핫 데이터 (1-5분 TTL)
- **L2**: 중간 빈도 데이터 (10분-1시간 TTL) 
- **Long-term**: 거의 변하지 않는 데이터 (24시간 TTL)

### 2. 메모리 관리
- **LRU 정책**: 자동 eviction
- **크기 제한**: 메모리 사용량 제어
- **통계 기반**: 히트율 모니터링

### 3. 네트워크 최적화
- **압축**: JSON 직렬화
- **배치 처리**: multiGet 사용
- **연결 풀**: 커넥션 재사용

## 🔍 모니터링 & 알람

### 핵심 메트릭
- **히트율**: L1/L2 캐시별 적중률
- **응답시간**: 평균/P95/P99 응답시간
- **메모리 사용량**: 캐시 크기 및 eviction 횟수
- **TPS**: 초당 처리 요청 수

### 성능 기준
- **히트율**: >90% (우수), >70% (양호), <50% (개선 필요)
- **응답시간**: L1 <1ms, L2 <5ms
- **Eviction율**: <10% (전체 요청 대비)

### 알람 조건
- 히트율 50% 미만 지속
- 평균 응답시간 100ms 초과
- 메모리 사용률 90% 초과
- Redis 연결 실패

## 🚀 성능 테스트

### 부하 테스트 예제
```bash
# 동시 사용자 100명, 1000회 요청
curl -X POST "http://localhost:8080/api/cache/performance-test?iterations=1000&concurrency=100"

# 캐시 예열
curl -X POST "http://localhost:8080/api/cache/warmup"

# 배치 조회 테스트
curl -X POST "http://localhost:8080/api/cache/users/batch" \
  -H "Content-Type: application/json" \
  -d '["user1", "user2", "user3", "user4", "user5"]'
```

### 예상 성능 결과
```json
{
  "totalRequests": 1000,
  "concurrency": 100,
  "totalTimeMs": 2500,
  "averageResponseTimeMs": 2.5,
  "requestsPerSecond": 400.0
}
```

## ⚙️ 설정 가이드

### application.yml
```yaml
spring:
  cache:
    type: caffeine
  redis:
    host: localhost
    port: 6379
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 20
        max-idle: 8
        min-idle: 0
```

### 환경별 튜닝
- **개발환경**: 작은 캐시 크기, 짧은 TTL
- **스테이징**: 운영 환경의 50% 수준
- **운영환경**: 최대 성능 설정, 장기 TTL

## 🔧 트러블슈팅

### 자주 발생하는 문제
1. **캐시 미스율 높음**: TTL 너무 짧음, 캐시 크기 부족
2. **메모리 부족**: 최대 크기 설정 필요
3. **Redis 연결 실패**: 연결 풀 설정 확인
4. **직렬화 오류**: JSON 직렬화 가능한 객체인지 확인

### 성능 튜닝 가이드
1. **히트율 개선**: 캐시 크기 증가, TTL 연장
2. **응답시간 개선**: L1 캐시 적극 활용
3. **메모리 최적화**: eviction 정책 조정
4. **네트워크 최적화**: 배치 처리 활용

## 📚 관련 문서

- [Caffeine 공식 문서](https://github.com/ben-manes/caffeine)
- [Spring Cache 문서](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#cache)
- [Redis 성능 가이드](https://redis.io/docs/reference/optimization/)