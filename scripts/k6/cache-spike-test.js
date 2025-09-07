import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// 스파이크 테스트 전용 메트릭
const spikeResponseTime = new Trend('spike_response_time');
const spikeErrorRate = new Rate('spike_error_rate');
const recoveryTime = new Trend('recovery_time');

export const options = {
  scenarios: {
    // 스파이크 테스트 패턴
    traffic_spike: {
      executor: 'ramping-vus',
      startVUs: 10,
      stages: [
        { duration: '1m', target: 10 },    // 정상 트래픽
        { duration: '30s', target: 1000 }, // 급격한 스파이크
        { duration: '2m', target: 1000 },  // 스파이크 유지
        { duration: '30s', target: 10 },   // 급격한 감소
        { duration: '2m', target: 10 },    // 회복 모니터링
      ],
    },
  },

  thresholds: {
    http_req_duration: ['p(95)<3000'],     // 스파이크 중 95%는 3초 이내
    spike_error_rate: ['rate<0.15'],       // 스파이크 중 에러율 15% 미만
    spike_response_time: ['p(90)<2000'],   // 스파이크 응답시간 90%는 2초 이내
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';

// 스파이크 시 주로 접근되는 인기 데이터
const popularUsers = Array.from({ length: 50 }, (_, i) => `user${i + 1}`);
const popularProducts = Array.from({ length: 20 }, (_, i) => `product${i + 1}`);
const trendingData = Array.from({ length: 10 }, (_, i) => `trending${i + 1}`);

export default function () {
  const currentStage = getCurrentStage();
  
  // 스테이지별 테스트 패턴
  switch (currentStage) {
    case 'normal':
      runNormalTraffic();
      break;
    case 'spike':
      runSpikeTraffic();
      break;
    case 'recovery':
      runRecoveryTest();
      break;
    default:
      runNormalTraffic();
  }
}

function getCurrentStage() {
  // 현재 시점을 기준으로 테스트 스테이지 판단
  const elapsed = Date.now() - __ENV.TEST_START_TIME;
  const elapsedMinutes = elapsed / (1000 * 60);
  
  if (elapsedMinutes < 1) return 'normal';
  if (elapsedMinutes < 4) return 'spike';  // 1분~4분: 스파이크 구간
  return 'recovery';
}

function runNormalTraffic() {
  // 정상 트래픽 패턴
  const testType = Math.random();
  
  if (testType < 0.7) {
    const userId = popularUsers[Math.floor(Math.random() * popularUsers.length)];
    getUserWithNormalLoad(userId);
  } else {
    const productId = popularProducts[Math.floor(Math.random() * popularProducts.length)];
    getProductWithNormalLoad(productId);
  }
  
  sleep(Math.random() * 3 + 1); // 1-4초 대기
}

function runSpikeTraffic() {
  // 스파이크 트래픽 - 매우 집중적이고 빠른 요청
  const spikeStart = Date.now();
  
  // 80% 확률로 인기 데이터에 집중
  if (Math.random() < 0.8) {
    const userId = popularUsers[Math.floor(Math.random() * 5)]; // 상위 5명에 집중
    const response = getUserWithSpike(userId);
    recordSpikeMetrics(response, spikeStart);
  } else {
    const productId = popularProducts[Math.floor(Math.random() * 3)]; // 상위 3개에 집중
    const response = getProductWithSpike(productId);
    recordSpikeMetrics(response, spikeStart);
  }
  
  sleep(Math.random() * 0.5); // 매우 짧은 대기시간
}

function runRecoveryTest() {
  // 회복 단계 테스트
  const recoveryStart = Date.now();
  
  const userId = popularUsers[Math.floor(Math.random() * popularUsers.length)];
  const response = getUserWithRecovery(userId);
  
  if (response.status === 200) {
    recoveryTime.add(Date.now() - recoveryStart);
  }
  
  sleep(Math.random() * 2 + 1); // 1-3초 대기
}

function getUserWithNormalLoad(userId) {
  const response = http.get(`${BASE_URL}/api/cache/users/${userId}`, {
    tags: { traffic_type: 'normal', api: 'user' },
    timeout: '5s',
  });
  
  check(response, {
    'normal user status is 200': (r) => r.status === 200,
    'normal user response time < 1000ms': (r) => r.timings.duration < 1000,
  });
  
  return response;
}

function getProductWithNormalLoad(productId) {
  const response = http.get(`${BASE_URL}/api/cache/products/${productId}`, {
    tags: { traffic_type: 'normal', api: 'product' },
    timeout: '5s',
  });
  
  check(response, {
    'normal product status is 200': (r) => r.status === 200,
    'normal product response time < 1000ms': (r) => r.timings.duration < 1000,
  });
  
  return response;
}

function getUserWithSpike(userId) {
  const response = http.get(`${BASE_URL}/api/cache/users/${userId}`, {
    tags: { traffic_type: 'spike', api: 'user' },
    timeout: '10s',
  });
  
  const isSuccess = check(response, {
    'spike user request completed': (r) => r.status >= 200 && r.status < 500,
    'spike user response time acceptable': (r) => r.timings.duration < 5000,
  });
  
  if (!isSuccess) {
    spikeErrorRate.add(1);
  }
  
  return response;
}

function getProductWithSpike(productId) {
  const response = http.get(`${BASE_URL}/api/cache/products/${productId}`, {
    tags: { traffic_type: 'spike', api: 'product' },
    timeout: '10s',
  });
  
  const isSuccess = check(response, {
    'spike product request completed': (r) => r.status >= 200 && r.status < 500,
    'spike product response time acceptable': (r) => r.timings.duration < 5000,
  });
  
  if (!isSuccess) {
    spikeErrorRate.add(1);
  }
  
  return response;
}

function getUserWithRecovery(userId) {
  const response = http.get(`${BASE_URL}/api/cache/users/${userId}`, {
    tags: { traffic_type: 'recovery', api: 'user' },
    timeout: '8s',
  });
  
  check(response, {
    'recovery user status is 200': (r) => r.status === 200,
    'recovery user response time improving': (r) => r.timings.duration < 2000,
  });
  
  return response;
}

function recordSpikeMetrics(response, startTime) {
  spikeResponseTime.add(response.timings.duration);
  
  if (response.status >= 400) {
    spikeErrorRate.add(1);
  } else {
    spikeErrorRate.add(0);
  }
}

export function setup() {
  console.log('⚡ 스파이크 테스트 시작 - 트래픽 급증 시뮬레이션');
  
  // 캐시 예열로 정상 상태 만들기
  const warmupResponse = http.post(`${BASE_URL}/api/cache/warmup`);
  check(warmupResponse, {
    'spike test warmup successful': (r) => r.status === 200,
  });
  
  sleep(5);
  
  // 테스트 시작 시간 설정
  __ENV.TEST_START_TIME = Date.now();
  
  return { 
    testStarted: Date.now(),
    baselineMetrics: getBaselineMetrics()
  };
}

function getBaselineMetrics() {
  const response = http.get(`${BASE_URL}/api/cache/metrics/report`);
  if (response.status === 200) {
    return JSON.parse(response.body);
  }
  return null;
}

export function teardown(data) {
  const testDuration = (Date.now() - data.testStarted) / 1000;
  console.log(`⚡ 스파이크 테스트 완료 - 총 소요시간: ${testDuration}초`);
  
  // 최종 메트릭 비교
  const finalMetrics = http.get(`${BASE_URL}/api/cache/metrics/report`);
  if (finalMetrics.status === 200) {
    const final = JSON.parse(finalMetrics.body);
    console.log('📊 스파이크 테스트 후 시스템 상태:');
    console.log(`   Redis 히트율: ${(final.payload.redisMetrics.hitRate * 100).toFixed(2)}%`);
    console.log(`   전체 히트율: ${(final.payload.summary.overallHitRate * 100).toFixed(2)}%`);
    
    if (final.payload.summary.overallHitRate > 0.7) {
      console.log('✅ 시스템이 스파이크를 잘 처리했습니다.');
    } else {
      console.log('⚠️ 스파이크로 인한 성능 저하가 있었습니다.');
    }
  }
}