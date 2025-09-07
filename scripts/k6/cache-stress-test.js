import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// 스트레스 테스트 전용 메트릭
const errorRate = new Rate('error_rate');
const successfulRequests = new Counter('successful_requests');
const cacheOverflow = new Counter('cache_overflow');

export const options = {
  scenarios: {
    // 극한 부하 테스트
    extreme_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 500 },   // 2분간 500명까지
        { duration: '5m', target: 1000 },  // 5분간 1000명까지
        { duration: '5m', target: 1500 },  // 5분간 1500명까지
        { duration: '3m', target: 2000 },  // 3분간 2000명까지 (극한)
        { duration: '2m', target: 500 },   // 2분간 500명으로 감소
        { duration: '1m', target: 0 },     // 1분간 0명으로 감소
      ],
    },
  },

  thresholds: {
    http_req_duration: ['p(95)<2000', 'p(99)<5000'], // 더 관대한 임계값
    http_req_failed: ['rate<0.05'],                   // 실패율 5% 미만
    error_rate: ['rate<0.1'],                         // 에러율 10% 미만
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const users = Array.from({ length: 10000 }, (_, i) => `user${i + 1}`);
const products = Array.from({ length: 5000 }, (_, i) => `product${i + 1}`);

export default function () {
  const testPattern = Math.random();
  
  try {
    if (testPattern < 0.6) {
      // 60% - 대량 사용자 조회
      massUserQuery();
    } else if (testPattern < 0.9) {
      // 30% - 대량 상품 조회
      massProductQuery();
    } else {
      // 10% - 배치 처리 부하
      batchLoadTest();
    }
    
    successfulRequests.add(1);
  } catch (error) {
    errorRate.add(1);
    console.error('Request failed:', error);
  }
  
  sleep(0.1); // 짧은 대기시간으로 높은 RPS 달성
}

function massUserQuery() {
  const userId = users[Math.floor(Math.random() * users.length)];
  const response = http.get(`${BASE_URL}/api/cache/users/${userId}`, {
    timeout: '10s',
    tags: { test_type: 'mass_user' },
  });
  
  const isSuccess = check(response, {
    'status is 200': (r) => r.status === 200,
    'response time acceptable': (r) => r.timings.duration < 5000,
  });
  
  if (!isSuccess) {
    errorRate.add(1);
  }
}

function massProductQuery() {
  const productId = products[Math.floor(Math.random() * products.length)];
  const response = http.get(`${BASE_URL}/api/cache/products/${productId}`, {
    timeout: '10s',
    tags: { test_type: 'mass_product' },
  });
  
  const isSuccess = check(response, {
    'status is 200': (r) => r.status === 200,
    'response time acceptable': (r) => r.timings.duration < 5000,
  });
  
  if (!isSuccess) {
    errorRate.add(1);
  }
}

function batchLoadTest() {
  const batchSize = Math.floor(Math.random() * 20) + 5; // 5-25개 배치
  const batchUsers = [];
  
  for (let i = 0; i < batchSize; i++) {
    batchUsers.push(users[Math.floor(Math.random() * users.length)]);
  }
  
  const response = http.post(
    `${BASE_URL}/api/cache/users/batch`,
    JSON.stringify(batchUsers),
    {
      headers: { 'Content-Type': 'application/json' },
      timeout: '15s',
      tags: { test_type: 'batch_load' },
    }
  );
  
  const isSuccess = check(response, {
    'batch status is 200': (r) => r.status === 200,
    'batch response time acceptable': (r) => r.timings.duration < 10000,
  });
  
  if (!isSuccess) {
    errorRate.add(1);
  }
}

export function setup() {
  console.log('🔥 스트레스 테스트 시작 - 극한 부하 테스트');
  
  // 캐시 예열
  const warmupResponse = http.post(`${BASE_URL}/api/cache/warmup`);
  check(warmupResponse, {
    'warmup successful': (r) => r.status === 200,
  });
  
  sleep(10); // 충분한 예열 시간
  
  return { testStarted: Date.now() };
}

export function teardown(data) {
  const testDuration = (Date.now() - data.testStarted) / 1000;
  console.log(`🏁 스트레스 테스트 완료 - 총 소요시간: ${testDuration}초`);
  
  // 최종 시스템 상태 확인
  const healthCheck = http.get(`${BASE_URL}/api/cache/metrics/report`);
  if (healthCheck.status === 200) {
    console.log('✅ 시스템이 스트레스 테스트를 성공적으로 통과했습니다.');
  } else {
    console.log('⚠️ 시스템이 불안정한 상태입니다.');
  }
}