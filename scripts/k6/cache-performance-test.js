import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// 커스텀 메트릭 정의
const cacheHitRate = new Rate('cache_hit_rate');
const cacheResponseTime = new Trend('cache_response_time');
const dbFallbackCount = new Counter('db_fallback_count');

// 테스트 설정
export const options = {
  scenarios: {
    // 1. 캐시 워밍업 단계
    warmup: {
      executor: 'per-vu-iterations',
      vus: 5,
      iterations: 20,
      startTime: '0s',
      tags: { phase: 'warmup' },
    },
    
    // 2. 점진적 부하 증가 테스트
    ramp_up: {
      executor: 'ramping-vus',
      startVUs: 10,
      stages: [
        { duration: '30s', target: 50 },   // 30초 동안 50명까지 증가
        { duration: '1m', target: 100 },   // 1분 동안 100명까지 증가
        { duration: '2m', target: 200 },   // 2분 동안 200명까지 증가
        { duration: '1m', target: 100 },   // 1분 동안 100명으로 감소
        { duration: '30s', target: 0 },    // 30초 동안 0명으로 감소
      ],
      startTime: '30s',
      tags: { phase: 'ramp_up' },
    },

    // 3. 고부하 지속 테스트
    stress: {
      executor: 'constant-vus',
      vus: 300,
      duration: '3m',
      startTime: '5m',
      tags: { phase: 'stress' },
    },

    // 4. 스파이크 테스트
    spike: {
      executor: 'ramping-vus',
      startVUs: 50,
      stages: [
        { duration: '10s', target: 500 },  // 급격한 증가
        { duration: '30s', target: 500 },  // 고부하 유지
        { duration: '10s', target: 50 },   // 급격한 감소
      ],
      startTime: '8m30s',
      tags: { phase: 'spike' },
    },
  },

  // 전역 임계값 설정
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],  // 95%: 500ms, 99%: 1s
    http_req_failed: ['rate<0.01'],                   // 실패율 1% 미만
    cache_hit_rate: ['rate>0.8'],                     // 캐시 히트율 80% 이상
    cache_response_time: ['p(95)<100'],               // 캐시 응답시간 95%: 100ms
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';

// 테스트 데이터 생성
const users = Array.from({ length: 1000 }, (_, i) => `user${i + 1}`);
const products = Array.from({ length: 500 }, (_, i) => `product${i + 1}`);
const hotDataKeys = Array.from({ length: 100 }, (_, i) => `hotdata${i + 1}`);

export default function () {
  const phase = __ENV.K6_SCENARIO || 'default';
  
  // 시나리오별 테스트 패턴
  switch (phase) {
    case 'warmup':
      runWarmupTest();
      break;
    case 'cache_hit':
      runCacheHitTest();
      break;
    case 'mixed_load':
      runMixedLoadTest();
      break;
    default:
      runDefaultTest();
  }
  
  sleep(Math.random() * 2 + 1); // 1-3초 랜덤 대기
}

function runWarmupTest() {
  // 캐시 예열을 위한 순차적 데이터 로드
  const userId = users[Math.floor(Math.random() * 100)]; // 상위 100개 사용자만
  const productId = products[Math.floor(Math.random() * 50)]; // 상위 50개 상품만
  
  getUserData(userId);
  getProductData(productId);
}

function runCacheHitTest() {
  // 캐시 히트율을 높이기 위해 제한된 데이터셋 사용
  const userId = users[Math.floor(Math.random() * 20)]; // 상위 20개 사용자
  const productId = products[Math.floor(Math.random() * 10)]; // 상위 10개 상품
  
  const userResponse = getUserData(userId);
  const productResponse = getProductData(productId);
  
  // 캐시 히트 여부 판단 (응답시간 기반)
  if (userResponse.timings.duration < 50) {
    cacheHitRate.add(1);
  } else {
    cacheHitRate.add(0);
    dbFallbackCount.add(1);
  }
}

function runMixedLoadTest() {
  const testType = Math.random();
  
  if (testType < 0.5) {
    // 50% - 사용자 데이터 조회
    const userId = users[Math.floor(Math.random() * users.length)];
    getUserData(userId);
  } else if (testType < 0.8) {
    // 30% - 상품 데이터 조회
    const productId = products[Math.floor(Math.random() * products.length)];
    getProductData(productId);
  } else if (testType < 0.95) {
    // 15% - 핫 데이터 조회
    const hotDataKey = hotDataKeys[Math.floor(Math.random() * hotDataKeys.length)];
    getHotData(hotDataKey);
  } else {
    // 5% - 배치 조회
    const batchUsers = users.slice(0, 5);
    getBatchUsers(batchUsers);
  }
}

function runDefaultTest() {
  // 기본 혼합 테스트 패턴
  runMixedLoadTest();
  
  // 10% 확률로 캐시 관리 작업
  if (Math.random() < 0.1) {
    getCacheMetrics();
  }
}

// API 호출 함수들
function getUserData(userId) {
  const response = http.get(`${BASE_URL}/api/cache/users/${userId}`, {
    tags: { api: 'user', cache_type: 'multilevel' },
  });
  
  check(response, {
    'user API status is 200': (r) => r.status === 200,
    'user API response time < 500ms': (r) => r.timings.duration < 500,
  });
  
  cacheResponseTime.add(response.timings.duration);
  return response;
}

function getProductData(productId) {
  const response = http.get(`${BASE_URL}/api/cache/products/${productId}`, {
    tags: { api: 'product', cache_type: 'multilevel' },
  });
  
  check(response, {
    'product API status is 200': (r) => r.status === 200,
    'product API response time < 500ms': (r) => r.timings.duration < 500,
  });
  
  cacheResponseTime.add(response.timings.duration);
  return response;
}

function getHotData(dataKey) {
  const response = http.get(`${BASE_URL}/api/cache/hotdata/${dataKey}`, {
    tags: { api: 'hotdata', cache_type: 'multilevel' },
  });
  
  check(response, {
    'hotdata API status is 200': (r) => r.status === 200,
    'hotdata API response time < 100ms': (r) => r.timings.duration < 100,
  });
  
  return response;
}

function getBatchUsers(userIds) {
  const response = http.post(
    `${BASE_URL}/api/cache/users/batch`,
    JSON.stringify(userIds),
    {
      headers: { 'Content-Type': 'application/json' },
      tags: { api: 'batch', cache_type: 'multilevel' },
    }
  );
  
  check(response, {
    'batch API status is 200': (r) => r.status === 200,
    'batch API response time < 1000ms': (r) => r.timings.duration < 1000,
  });
  
  return response;
}

function getCacheMetrics() {
  const response = http.get(`${BASE_URL}/api/cache/metrics/report`, {
    tags: { api: 'metrics', cache_type: 'monitoring' },
  });
  
  check(response, {
    'metrics API status is 200': (r) => r.status === 200,
  });
  
  return response;
}

// 테스트 시작 시 캐시 예열
export function setup() {
  console.log('🚀 캐시 성능 테스트 시작 - 캐시 예열 중...');
  
  const warmupResponse = http.post(`${BASE_URL}/api/cache/warmup`);
  check(warmupResponse, {
    'warmup API status is 200': (r) => r.status === 200,
  });
  
  sleep(5); // 예열 완료 대기
  
  console.log('✅ 캐시 예열 완료');
  return { warmupCompleted: true };
}

// 테스트 종료 후 결과 정리
export function teardown(data) {
  console.log('📊 테스트 완료 - 최종 메트릭 수집 중...');
  
  const finalMetrics = http.get(`${BASE_URL}/api/cache/metrics/report`);
  if (finalMetrics.status === 200) {
    const metrics = JSON.parse(finalMetrics.body);
    console.log('📈 최종 캐시 통계:', JSON.stringify(metrics.payload, null, 2));
  }
}