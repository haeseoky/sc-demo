package com.ocean.scdemo.aop.example.controller;

import com.ocean.scdemo.aop.annotation.PreventDuplicateExecution;
import com.ocean.scdemo.aop.example.dto.PaymentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 결제 관리 컨트롤러
 * 다양한 TTL 설정 예제
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    /**
     * 결제 처리 (기본 TTL 5초)
     *
     * @param request 결제 요청
     * @return 결제 결과
     */
    @PostMapping
    @PreventDuplicateExecution(keys = {"paymentId", "userId"})
    public ResponseEntity<String> processPayment(@RequestBody PaymentRequest request) {
        log.info("Processing payment - paymentId: {}, userId: {}",
                request.getPaymentId(), request.getUserId());

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return ResponseEntity.ok("Payment processed: " + request.getPaymentId());
    }

    /**
     * 결제 환불 (긴 TTL 30초)
     *
     * @param request 결제 요청
     * @return 환불 결과
     */
    @PostMapping("/refund")
    @PreventDuplicateExecution(
            keys = {"paymentId"},
            ttl = 30,
            message = "환불 처리가 이미 진행 중입니다. 30초 후 다시 시도해주세요."
    )
    public ResponseEntity<String> refundPayment(@RequestBody PaymentRequest request) {
        log.info("Processing refund - paymentId: {}", request.getPaymentId());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return ResponseEntity.ok("Refund processed: " + request.getPaymentId());
    }

    /**
     * 사용자별 결제 이력 조회 (짧은 TTL 2초)
     *
     * @param request 결제 요청
     * @return 결제 이력
     */
    @PostMapping("/history")
    @PreventDuplicateExecution(
            keys = {"userId"},
            ttl = 2,
            message = "결제 이력 조회 중입니다."
    )
    public ResponseEntity<String> getPaymentHistory(@RequestBody PaymentRequest request) {
        log.info("Fetching payment history - userId: {}", request.getUserId());

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return ResponseEntity.ok("Payment history for user: " + request.getUserId());
    }
}
