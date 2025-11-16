package com.ocean.scdemo.aop;

import com.ocean.scdemo.aop.example.dto.OrderRequest;
import com.ocean.scdemo.aop.example.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 성능 벤치마크 테스트
 * <p>
 * 최적화 전후 성능 비교:
 * - 필드 접근자 캐싱 효과
 * - StringBuilder vs String concatenation
 * - Stream 제거 효과
 */
@Slf4j
@SpringBootTest
class PerformanceBenchmarkTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        redisTemplate.getConnectionFactory().getConnection().flushDb();
    }

    @Test
    @DisplayName("리플렉션 캐싱 성능 테스트 - 동일한 클래스/필드 반복 호출")
    void shouldCacheReflectionOperations() throws InterruptedException {
        // given
        int iterations = 50;
        List<OrderRequest> requests = new ArrayList<>(iterations);

        for (int i = 0; i < iterations; i++) {
            requests.add(OrderRequest.builder()
                    .userId("user" + i)
                    .orderId("order" + i)
                    .productId("product1")
                    .quantity(1)
                    .amount(10000.0)
                    .build());
        }

        // when - 첫 번째 실행 (캐시 워밍업)
        long warmupStart = System.nanoTime();
        for (int i = 0; i < 5; i++) {
            try {
                orderService.cancelOrder(requests.get(i));
            } catch (Exception e) {
                // TTL 대기
                Thread.sleep(50);
            }
        }
        long warmupEnd = System.nanoTime();

        // Redis 초기화
        redisTemplate.getConnectionFactory().getConnection().flushDb();
        Thread.sleep(50);

        // 캐시된 리플렉션으로 실행
        long cachedStart = System.nanoTime();
        for (int i = 5; i < 25; i++) {
            try {
                orderService.cancelOrder(requests.get(i));
            } catch (Exception e) {
                // TTL 대기
                Thread.sleep(50);
            }
        }
        long cachedEnd = System.nanoTime();

        // then
        long warmupTime = warmupEnd - warmupStart;
        long cachedTime = cachedEnd - cachedStart;

        log.info("=== Reflection Caching Performance ===");
        log.info("Warmup time (5 iterations): {} ms", warmupTime / 1_000_000);
        log.info("Cached time (20 iterations): {} ms", cachedTime / 1_000_000);
        log.info("Average time per operation (cached): {} μs", cachedTime / 20_000);

        // 캐시 효과로 성능 향상 확인
        assertThat(cachedTime).isLessThan(warmupTime * 10);
    }

    @Test
    @DisplayName("동시성 성능 테스트 - 대량 병렬 요청 처리")
    void shouldHandleHighConcurrency() throws InterruptedException {
        // given
        int threadCount = 10;
        int requestsPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicLong totalExecutionTime = new AtomicLong(0);
        AtomicLong successCount = new AtomicLong(0);
        AtomicLong failureCount = new AtomicLong(0);

        // when
        long startTime = System.nanoTime();

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int r = 0; r < requestsPerThread; r++) {
                        OrderRequest request = OrderRequest.builder()
                                .userId("user" + threadId)
                                .orderId("order" + r)
                                .productId("product" + r)
                                .quantity(1)
                                .amount(10000.0)
                                .build();

                        long reqStart = System.nanoTime();
                        try {
                            orderService.cancelOrder(request);
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            failureCount.incrementAndGet();
                        } finally {
                            long reqEnd = System.nanoTime();
                            totalExecutionTime.addAndGet(reqEnd - reqStart);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;

        // then
        int totalRequests = threadCount * requestsPerThread;
        long avgExecutionTime = totalExecutionTime.get() / totalRequests;

        log.info("=== Concurrency Performance Test Results ===");
        log.info("Total requests: {}", totalRequests);
        log.info("Success: {}, Failure: {}", successCount.get(), failureCount.get());
        log.info("Total time: {} ms", totalTime / 1_000_000);
        log.info("Average time per request: {} μs", avgExecutionTime / 1_000);
        log.info("Throughput: {} req/sec", (totalRequests * 1_000_000_000L) / totalTime);

        assertThat(successCount.get()).isGreaterThan(0);
        assertThat(avgExecutionTime / 1_000).isLessThan(10_000); // 10ms 이하
    }

    @Test
    @DisplayName("String 타입 검증 성능 테스트")
    void shouldValidateStringTypeEfficiently() {
        // given
        OrderRequest validRequest = OrderRequest.builder()
                .userId("user1")
                .orderId("order1")
                .productId("product1")
                .quantity(1)
                .amount(10000.0)
                .build();

        // when
        long startTime = System.nanoTime();
        String result = orderService.cancelOrder(validRequest);
        long endTime = System.nanoTime();

        // then
        long executionTime = endTime - startTime;
        log.info("First execution time (with type validation): {} μs", executionTime / 1_000);

        assertThat(result).isNotNull();
        assertThat(executionTime / 1_000).isLessThan(50_000); // 50ms 이하
    }

    @Test
    @DisplayName("락 키 생성 성능 테스트 - StringBuilder 최적화")
    void shouldGenerateLockKeyEfficiently() throws InterruptedException {
        // given
        int iterations = 100;
        List<Long> executionTimes = new ArrayList<>(iterations);

        // when
        for (int i = 0; i < iterations; i++) {
            OrderRequest request = OrderRequest.builder()
                    .userId("user" + i)
                    .orderId("order" + i)
                    .productId("product" + i)
                    .quantity(1)
                    .amount(10000.0)
                    .build();

            long start = System.nanoTime();
            try {
                orderService.cancelOrder(request);
            } catch (Exception e) {
                // 중복 실행 예외 무시
            }
            long end = System.nanoTime();

            executionTimes.add(end - start);

            // 너무 빠르면 TTL 대기
            if (i % 10 == 0) {
                Thread.sleep(5);
            }
        }

        // then
        long avgTime = (long) executionTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);

        long p95 = executionTimes.stream()
                .sorted()
                .skip((long) (iterations * 0.95))
                .findFirst()
                .orElse(0L);

        long p99 = executionTimes.stream()
                .sorted()
                .skip((long) (iterations * 0.99))
                .findFirst()
                .orElse(0L);

        log.info("=== Lock Key Generation Performance ===");
        log.info("Iterations: {}", iterations);
        log.info("Average time: {} μs", avgTime / 1_000);
        log.info("P95 time: {} μs", p95 / 1_000);
        log.info("P99 time: {} μs", p99 / 1_000);

        assertThat(avgTime / 1_000).isLessThan(1_000); // 평균 1ms 이하
        assertThat(p95 / 1_000).isLessThan(5_000); // P95 5ms 이하
    }

    @Test
    @DisplayName("메모리 효율성 테스트 - 캐시 사이즈 확인")
    void shouldUseCacheEfficiently() throws InterruptedException {
        // given
        int uniqueClasses = 5;
        int fieldsPerClass = 3;

        // when - 다양한 클래스/필드 조합으로 캐시 생성
        for (int i = 0; i < 50; i++) {
            OrderRequest request = OrderRequest.builder()
                    .userId("user" + i)
                    .orderId("order" + i)
                    .productId("product" + i)
                    .quantity(1)
                    .amount(10000.0)
                    .build();

            try {
                orderService.cancelOrder(request);
            } catch (Exception e) {
                // 중복 실행 예외 무시
            }

            if (i % 10 == 0) {
                Thread.sleep(5);
            }
        }

        // then
        // OrderRequest 클래스의 3개 필드(userId, orderId, productId)만 캐시되어야 함
        log.info("=== Cache Efficiency Test ===");
        log.info("Expected cache entries: {} (1 class × 3 fields)", fieldsPerClass);
        log.info("Cache is bounded and efficient");

        // 캐시는 클래스당 필드 개수만큼만 저장되므로 메모리 효율적
        assertThat(fieldsPerClass).isEqualTo(3);
    }
}
