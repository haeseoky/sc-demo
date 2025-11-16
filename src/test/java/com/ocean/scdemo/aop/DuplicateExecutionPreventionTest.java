package com.ocean.scdemo.aop;

import com.ocean.scdemo.aop.example.dto.OrderRequest;
import com.ocean.scdemo.aop.example.dto.PaymentRequest;
import com.ocean.scdemo.aop.example.service.OrderService;
import com.ocean.scdemo.aop.exception.DuplicateExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 중복 실행 방지 기능 테스트
 */
@SpringBootTest
class DuplicateExecutionPreventionTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        // Redis 초기화
        redisTemplate.getConnectionFactory().getConnection().flushDb();
    }

    @Test
    @DisplayName("중복 실행이 방지되어야 한다")
    void shouldPreventDuplicateExecution() {
        // given
        OrderRequest request = OrderRequest.builder()
                .userId("user1")
                .orderId("order1")
                .productId("product1")
                .quantity(1)
                .amount(10000.0)
                .build();

        // when & then
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);

        // 동시에 2개의 요청 실행
        executorService.submit(() -> {
            try {
                orderService.createOrder(request);
                successCount.incrementAndGet();
            } catch (DuplicateExecutionException e) {
                failureCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });

        executorService.submit(() -> {
            try {
                Thread.sleep(100); // 약간의 딜레이
                orderService.createOrder(request);
                successCount.incrementAndGet();
            } catch (DuplicateExecutionException | InterruptedException e) {
                failureCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 하나는 성공, 하나는 실패해야 함
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);

        executorService.shutdown();
    }

    @Test
    @DisplayName("다른 키를 가진 요청은 동시에 실행될 수 있어야 한다")
    void shouldAllowDifferentKeys() {
        // given
        OrderRequest request1 = OrderRequest.builder()
                .userId("user1")
                .orderId("order1")
                .productId("product1")
                .quantity(1)
                .amount(10000.0)
                .build();

        OrderRequest request2 = OrderRequest.builder()
                .userId("user1")
                .orderId("order2")  // 다른 orderId
                .productId("product1")
                .quantity(1)
                .amount(10000.0)
                .build();

        // when & then
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);

        executorService.submit(() -> {
            try {
                orderService.createOrder(request1);
                successCount.incrementAndGet();
            } catch (Exception e) {
                // 예외 발생하면 안됨
            } finally {
                latch.countDown();
            }
        });

        executorService.submit(() -> {
            try {
                orderService.createOrder(request2);
                successCount.incrementAndGet();
            } catch (Exception e) {
                // 예외 발생하면 안됨
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 둘 다 성공해야 함
        assertThat(successCount.get()).isEqualTo(2);

        executorService.shutdown();
    }

    @Test
    @DisplayName("TTL 만료 후에는 다시 실행할 수 있어야 한다")
    void shouldAllowExecutionAfterTTL() throws InterruptedException {
        // given
        OrderRequest request = OrderRequest.builder()
                .userId("user2")
                .orderId("order2")
                .productId("product1")
                .quantity(1)
                .amount(10000.0)
                .build();

        // when
        String result1 = orderService.createOrder(request);

        // TTL(5초)이 지날 때까지 대기
        Thread.sleep(6000);

        String result2 = orderService.createOrder(request);

        // then
        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
    }

    @Test
    @DisplayName("여러 키를 조합하여 락이 생성되어야 한다")
    void shouldCreateLockWithMultipleKeys() throws InterruptedException {
        // given
        OrderRequest request = OrderRequest.builder()
                .userId("user3")
                .orderId("order3")
                .productId("product3")
                .quantity(1)
                .amount(10000.0)
                .build();

        // when - 비동기로 첫 번째 요청 실행
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> orderService.cancelOrder(request));

        // 첫 번째 요청이 시작될 때까지 대기
        Thread.sleep(100);

        // then - 두 번째 요청은 중복으로 차단되어야 함
        assertThatThrownBy(() -> orderService.cancelOrder(request))
                .isInstanceOf(DuplicateExecutionException.class)
                .hasMessageContaining("동일한 주문 취소 요청이 이미 처리 중입니다");

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("null 값이 포함된 파라미터도 처리할 수 있어야 한다")
    void shouldHandleNullValues() throws InterruptedException {
        // given
        OrderRequest request = OrderRequest.builder()
                .userId("user4")
                .orderId("order4")
                .productId(null)  // null 값
                .quantity(1)
                .amount(10000.0)
                .build();

        // when - 비동기로 첫 번째 요청 실행
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> orderService.cancelOrder(request));

        // 첫 번째 요청이 시작될 때까지 대기
        Thread.sleep(100);

        // then - 중복 실행 시도는 차단되어야 함
        assertThatThrownBy(() -> orderService.cancelOrder(request))
                .isInstanceOf(DuplicateExecutionException.class);

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }
}
