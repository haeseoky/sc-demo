package com.ocean.scdemo.aop;

import com.ocean.scdemo.aop.annotation.PreventDuplicateExecution;
import com.ocean.scdemo.aop.exception.DuplicateExecutionException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 테스트 클래스 내부에서 @PreventDuplicateExecution 어노테이션을 적용하고 테스트하는 예제
 *
 * 핵심: AOP는 Spring 프록시를 통해 동작하므로, 테스트용 Bean을 정의하여 사용해야 함
 *
 * 방법: @Service 어노테이션을 사용하여 Spring 컴포넌트 스캔에 의해 Bean 등록
 */
@Slf4j
@SpringBootTest
class InternalBeanAnnotationTest {

    @Autowired
    private TestAnnotationService testAnnotationService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        redisTemplate.getConnectionFactory().getConnection().flushDb();
    }

    @Test
    @DisplayName("테스트용 내부 Bean에서 중복 실행이 방지되어야 한다")
    void shouldPreventDuplicateExecutionInInternalBean() throws InterruptedException {
        // given
        SimpleRequest request = new SimpleRequest("testUser1", "testOrder1");

        // when - 비동기로 첫 번째 요청 실행
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            log.info("Starting first request...");
            testAnnotationService.processRequest(request);
            log.info("First request completed");
        });

        // 첫 번째 요청이 시작될 때까지 대기
        Thread.sleep(200);

        // then - 두 번째 요청은 중복으로 차단되어야 함
        assertThatThrownBy(() -> testAnnotationService.processRequest(request))
                .isInstanceOf(DuplicateExecutionException.class)
                .hasMessageContaining("테스트 중복 실행 방지");

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("다른 파라미터는 동시 실행이 가능해야 한다")
    void shouldAllowDifferentParameters() throws InterruptedException {
        // given
        SimpleRequest request1 = new SimpleRequest("user1", "order1");
        SimpleRequest request2 = new SimpleRequest("user1", "order2"); // orderId 다름

        // when
        ExecutorService executor = Executors.newFixedThreadPool(2);
        AtomicInteger successCount = new AtomicInteger(0);

        executor.submit(() -> {
            try {
                testAnnotationService.processRequest(request1);
                successCount.incrementAndGet();
            } catch (Exception e) {
                log.error("Request 1 failed", e);
            }
        });

        executor.submit(() -> {
            try {
                testAnnotationService.processRequest(request2);
                successCount.incrementAndGet();
            } catch (Exception e) {
                log.error("Request 2 failed", e);
            }
        });

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // then - 둘 다 성공해야 함
        assertThat(successCount.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("커스텀 TTL이 적용되어야 한다")
    void shouldApplyCustomTTL() throws InterruptedException {
        // given
        ShortTTLRequest request = new ShortTTLRequest("user1", "data1");

        // when - 첫 번째 요청
        testAnnotationService.processWithShortTTL(request);

        // TTL 2초 후에는 다시 실행 가능해야 함
        Thread.sleep(2500);

        // then - 다시 실행 성공
        String result = testAnnotationService.processWithShortTTL(request);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("메소드명 기반 락이 동작해야 한다")
    void shouldWorkWithMethodNameBasedLock() throws InterruptedException {
        // when - 비동기로 첫 번째 요청
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> testAnnotationService.batchProcess());
        Thread.sleep(200);

        // then - 두 번째 요청은 차단
        assertThatThrownBy(() -> testAnnotationService.batchProcess())
                .isInstanceOf(DuplicateExecutionException.class)
                .hasMessageContaining("배치 작업 실행 중");

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("null 값을 포함한 파라미터도 처리해야 한다")
    void shouldHandleNullParameters() throws InterruptedException {
        // given
        SimpleRequest request = new SimpleRequest("user1", null);

        // when - 비동기로 첫 번째 요청
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> testAnnotationService.processRequest(request));
        Thread.sleep(200);

        // then - null도 키로 처리되어 중복 차단
        assertThatThrownBy(() -> testAnnotationService.processRequest(request))
                .isInstanceOf(DuplicateExecutionException.class);

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("여러 필드를 키로 조합해야 한다")
    void shouldCombineMultipleFieldsAsKey() throws InterruptedException {
        // given
        MultiKeyRequest request = new MultiKeyRequest("user1", "order1", "product1");

        // when - 비동기로 첫 번째 요청
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> testAnnotationService.processMultiKey(request));
        Thread.sleep(200);

        // then - 3개 필드 조합으로 중복 차단
        assertThatThrownBy(() -> testAnnotationService.processMultiKey(request))
                .isInstanceOf(DuplicateExecutionException.class);

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    // ========== 테스트용 DTO (public static inner class) ==========

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimpleRequest {
        private String userId;
        private String orderId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShortTTLRequest {
        private String userId;
        private String dataId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MultiKeyRequest {
        private String userId;
        private String orderId;
        private String productId;
    }
}

/**
 * 테스트용 서비스 Bean
 *
 * 핵심: @Service 어노테이션으로 Spring Bean 등록
 *      -> AOP 프록시가 적용되어 @PreventDuplicateExecution 동작
 */
@Slf4j
@Service
class TestAnnotationService {

    /**
     * 기본 테스트 - 2개 키 조합, 5초 TTL
     */
    @PreventDuplicateExecution(
            keys = {"userId", "orderId"},
            ttl = 5,
            message = "테스트 중복 실행 방지"
    )
    public String processRequest(InternalBeanAnnotationTest.SimpleRequest request) {
        log.info("Processing request - userId: {}, orderId: {}",
                request.getUserId(), request.getOrderId());
        try {
            Thread.sleep(1000);  // 1초 처리 시간
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "processed: " + request.getOrderId();
    }

    /**
     * 짧은 TTL 테스트 - 2초 TTL
     */
    @PreventDuplicateExecution(
            keys = {"userId", "dataId"},
            ttl = 2,
            message = "짧은 TTL 테스트"
    )
    public String processWithShortTTL(InternalBeanAnnotationTest.ShortTTLRequest request) {
        log.info("Processing with short TTL - userId: {}, dataId: {}",
                request.getUserId(), request.getDataId());
        try {
            Thread.sleep(500);  // 0.5초 처리 시간
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "done";
    }

    /**
     * 메소드명 기반 락 테스트
     */
    @PreventDuplicateExecution(
            keys = {},
            useMethodName = true,
            ttl = 3,
            message = "배치 작업 실행 중"
    )
    public String batchProcess() {
        log.info("Running batch process...");
        try {
            Thread.sleep(2000);  // 2초 처리 시간
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "batch completed";
    }

    /**
     * 3개 필드 조합 테스트
     */
    @PreventDuplicateExecution(
            keys = {"userId", "orderId", "productId"},
            ttl = 5,
            message = "다중 키 테스트"
    )
    public String processMultiKey(InternalBeanAnnotationTest.MultiKeyRequest request) {
        log.info("Processing multi-key - userId: {}, orderId: {}, productId: {}",
                request.getUserId(), request.getOrderId(), request.getProductId());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "multi-key processed";
    }
}
