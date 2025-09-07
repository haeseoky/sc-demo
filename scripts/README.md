# 🚀 K6 캐시 성능 테스트 가이드

## 📋 개요

K6를 사용하여 다단계 캐시 시스템의 성능을 측정하고 분석하는 테스트 스크립트 모음입니다.

## 🏗️ 디렉토리 구조

```
scripts/
├── k6/                              # K6 테스트 스크립트
│   ├── cache-performance-test.js    # 종합 성능 테스트
│   ├── cache-stress-test.js         # 스트레스 테스트
│   └── cache-spike-test.js          # 스파이크 테스트
├── grafana/                         # Grafana 설정
│   ├── dashboards/                  # 대시보드 설정
│   └── datasources/                 # 데이터소스 설정
├── results/                         # 테스트 결과 저장
├── docker-compose.k6.yml           # Docker Compose 설정
└── README.md                        # 이 문서
```

## 🎯 테스트 시나리오

### 1. 📊 종합 성능 테스트 (cache-performance-test.js)
- **목적**: 전체적인 캐시 성능 측정
- **시나리오**: 
  - 워밍업 (5 VUs, 20 iterations)
  - 점진적 부하 증가 (10→200 VUs)
  - 고부하 지속 (300 VUs, 3분)
  - 스파이크 (50→500 VUs)
- **측정 메트릭**: 히트율, 응답시간, TPS

### 2. 🔥 스트레스 테스트 (cache-stress-test.js)  
- **목적**: 극한 부하 상황에서의 시스템 안정성
- **시나리오**: 점진적으로 2000 VUs까지 증가
- **측정 메트릭**: 에러율, 시스템 복구 능력

### 3. ⚡ 스파이크 테스트 (cache-spike-test.js)
- **목적**: 급격한 트래픽 증가 시 대응 능력
- **시나리오**: 10 VUs에서 1000 VUs로 급증 후 복구
- **측정 메트릭**: 스파이크 중 응답시간, 복구 시간

## 🚀 빠른 시작

### 1. 애플리케이션 실행
```bash
# Spring Boot 애플리케이션 실행
./gradlew bootRun

# 또는 백그라운드 실행
./gradlew bootRun &
```

### 2. 기본 성능 테스트
```bash
cd scripts

# 단순 실행 (결과는 콘솔에 출력)
docker-compose -f docker-compose.k6.yml --profile performance run k6

# 또는 직접 k6 실행
docker run --rm -v $(pwd)/k6:/scripts grafana/k6:latest \
  run -e BASE_URL=http://host.docker.internal:8080 \
  /scripts/cache-performance-test.js
```

### 3. 스트레스 테스트
```bash
docker-compose -f docker-compose.k6.yml --profile stress run k6-stress
```

### 4. 스파이크 테스트  
```bash
docker-compose -f docker-compose.k6.yml --profile spike run k6-spike
```

## 📊 실시간 모니터링 (Grafana + InfluxDB)

### 1. 모니터링 환경 구성
```bash
# InfluxDB + Grafana 실행
docker-compose -f docker-compose.k6.yml --profile monitoring up -d

# 실시간 모니터링과 함께 테스트 실행
docker-compose -f docker-compose.k6.yml --profile live run k6-live
```

### 2. 대시보드 접속
- **Grafana**: http://localhost:3000
  - 사용자: admin / 비밀번호: admin123
- **InfluxDB**: http://localhost:8086
  - 사용자: k6 / 비밀번호: k6password

### 3. 커스텀 대시보드 추가
```bash
# Grafana에서 Import Dashboard 사용
# 대시보드 ID: 2587 (K6 Load Testing Results)
```

## 📈 고급 테스트 옵션

### 1. 환경변수 설정
```bash
# 테스트 대상 URL 변경
export BASE_URL=http://your-server:8080

# 테스트 강도 조절
export TEST_DURATION=10m
export MAX_VUS=500
```

### 2. 커스텀 실행 옵션
```bash
# 특정 시나리오만 실행
docker run --rm -v $(pwd)/k6:/scripts grafana/k6:latest \
  run --scenarios warmup /scripts/cache-performance-test.js

# 결과를 JSON으로 저장
docker run --rm -v $(pwd):/scripts -v $(pwd)/results:/results grafana/k6:latest \
  run --out json=/results/my-test.json /scripts/cache-performance-test.js

# InfluxDB로 실시간 전송
docker run --rm -v $(pwd)/k6:/scripts grafana/k6:latest \
  run --out influxdb=http://localhost:8086/k6 /scripts/cache-performance-test.js
```

### 3. 배치 테스트 실행
```bash
#!/bin/bash
# run-all-tests.sh

echo "🚀 캐시 성능 테스트 시작"

# 1. 성능 테스트
docker-compose -f docker-compose.k6.yml --profile performance run k6

# 2. 스트레스 테스트  
docker-compose -f docker-compose.k6.yml --profile stress run k6-stress

# 3. 스파이크 테스트
docker-compose -f docker-compose.k6.yml --profile spike run k6-spike

echo "✅ 모든 테스트 완료"
```

## 📊 결과 분석

### 주요 메트릭
- **http_req_duration**: HTTP 요청 응답시간
- **http_req_failed**: HTTP 요청 실패율
- **cache_hit_rate**: 캐시 히트율 (커스텀 메트릭)
- **cache_response_time**: 캐시 응답시간 (커스텀 메트릭)
- **iterations**: 초당 실행된 스크립트 반복 횟수

### 성능 기준
```javascript
// 테스트 통과 기준
thresholds: {
  'http_req_duration': ['p(95)<500', 'p(99)<1000'],  // 95%: 500ms, 99%: 1s
  'http_req_failed': ['rate<0.01'],                   // 실패율 1% 미만
  'cache_hit_rate': ['rate>0.8'],                     // 히트율 80% 이상
}
```

### 결과 해석
- **좋은 성능**: 
  - 히트율 > 90%
  - P95 응답시간 < 100ms
  - 실패율 < 0.1%
- **개선 필요**:
  - 히트율 < 70%
  - P95 응답시간 > 500ms
  - 실패율 > 1%

## 🛠️ 문제 해결

### 일반적인 문제

1. **연결 실패**
```bash
# 네트워크 확인
docker network ls

# 호스트 연결 테스트
curl http://host.docker.internal:8080/api/cache/users/user1
```

2. **메모리 부족**
```bash
# Docker 메모리 할당 확인/증가
docker system info | grep Memory

# 불필요한 컨테이너 정리
docker system prune -f
```

3. **포트 충돌**
```bash
# 포트 사용 확인
lsof -i :8080
lsof -i :3000
lsof -i :8086

# 포트 변경은 docker-compose.k6.yml에서 수정
```

### 성능 최적화 팁

1. **캐시 예열**
```bash
# 테스트 전 캐시 예열
curl -X POST http://localhost:8080/api/cache/warmup
```

2. **데이터베이스 최적화**
- 충분한 메모리 할당
- 연결 풀 크기 조정
- 인덱스 최적화

3. **네트워크 최적화**  
- Docker 네트워크 설정
- DNS 캐시 설정
- Keep-alive 연결 사용

## 📚 추가 리소스

- [K6 공식 문서](https://k6.io/docs/)
- [Grafana 대시보드](https://grafana.com/grafana/dashboards/)
- [InfluxDB 문서](https://docs.influxdata.com/influxdb/)
- [캐시 성능 최적화 가이드](../src/main/java/com/ocean/scdemo/cache/README.md)

## 🎯 다음 단계

1. **프로덕션 환경 테스트**
   - 실제 운영 환경에서 테스트
   - 보안 설정 적용

2. **CI/CD 통합**
   - GitHub Actions 연동
   - 자동화된 성능 회귀 테스트

3. **커스텀 메트릭 추가**
   - 비즈니스 로직 메트릭
   - 상세한 캐시 분석

4. **알람 설정**
   - 성능 저하 시 자동 알림
   - Slack/이메일 통합