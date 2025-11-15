package com.ocean.scdemo.aop.example.service;

import com.ocean.scdemo.aop.annotation.PreventDuplicateExecution;
import com.ocean.scdemo.aop.example.dto.OrderRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 주문 처리 서비스
 * Service 레이어에서 중복 실행 방지 기능을 사용하는 예제
 */
@Slf4j
@Service
public class OrderService {

    /**
     * 주문 생성
     * userId와 orderId를 조합하여 5초간 중복 실행 방지
     *
     * @param request 주문 요청
     * @return 주문 결과 메시지
     */
    @PreventDuplicateExecution(
            keys = {"userId", "orderId"},
            ttl = 5,
            message = "동일한 주문이 이미 처리 중입니다. 잠시 후 다시 시도해주세요."
    )
    public String createOrder(OrderRequest request) {
        log.info("Creating order - userId: {}, orderId: {}", request.getUserId(), request.getOrderId());

        // 주문 처리 시뮬레이션 (3초 소요)
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Order processing interrupted", e);
        }

        log.info("Order created successfully - orderId: {}", request.getOrderId());
        return "Order created: " + request.getOrderId();
    }

    /**
     * 주문 취소
     * userId, orderId, productId를 조합하여 10초간 중복 실행 방지
     *
     * @param request 주문 요청
     * @return 취소 결과 메시지
     */
    @PreventDuplicateExecution(
            keys = {"userId", "orderId", "productId"},
            ttl = 10,
            message = "동일한 주문 취소 요청이 이미 처리 중입니다."
    )
    public String cancelOrder(OrderRequest request) {
        log.info("Canceling order - userId: {}, orderId: {}, productId: {}",
                request.getUserId(), request.getOrderId(), request.getProductId());

        // 취소 처리 시뮬레이션 (2초 소요)
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Order cancellation interrupted", e);
        }

        log.info("Order canceled successfully - orderId: {}", request.getOrderId());
        return "Order canceled: " + request.getOrderId();
    }
}
