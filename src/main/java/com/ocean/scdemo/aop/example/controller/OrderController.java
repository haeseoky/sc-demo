package com.ocean.scdemo.aop.example.controller;

import com.ocean.scdemo.aop.annotation.PreventDuplicateExecution;
import com.ocean.scdemo.aop.example.dto.OrderRequest;
import com.ocean.scdemo.aop.example.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 주문 관리 컨트롤러
 * Controller 레이어에서 중복 실행 방지 기능을 사용하는 예제
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Controller 레벨에서 중복 실행 방지
     * userId와 orderId를 조합하여 5초간 중복 요청 차단
     *
     * @param request 주문 요청
     * @return 주문 생성 결과
     */
    @PostMapping
    @PreventDuplicateExecution(
            keys = {"userId", "orderId"},
            ttl = 5,
            message = "동일한 주문 요청이 이미 처리 중입니다."
    )
    public ResponseEntity<String> createOrderInController(@RequestBody OrderRequest request) {
        log.info("Controller - Received order request: {}", request);

        // 실제로는 서비스 레이어를 호출하지만, Controller 레벨에서 이미 중복 체크됨
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return ResponseEntity.ok("Order created in controller: " + request.getOrderId());
    }

    /**
     * Service 레벨에서 중복 실행 방지
     * Service 메소드에 @PreventDuplicateExecution이 적용되어 있음
     *
     * @param request 주문 요청
     * @return 주문 생성 결과
     */
    @PostMapping("/service")
    public ResponseEntity<String> createOrderInService(@RequestBody OrderRequest request) {
        log.info("Controller - Delegating to service layer: {}", request);
        String result = orderService.createOrder(request);
        return ResponseEntity.ok(result);
    }

    /**
     * Service 레벨에서 주문 취소 (여러 키 사용)
     *
     * @param request 주문 요청
     * @return 주문 취소 결과
     */
    @DeleteMapping
    public ResponseEntity<String> cancelOrder(@RequestBody OrderRequest request) {
        log.info("Controller - Cancel order request: {}", request);
        String result = orderService.cancelOrder(request);
        return ResponseEntity.ok(result);
    }

    /**
     * 메소드명만으로 중복 실행 방지 (파라미터 무관)
     *
     * @return 실행 결과
     */
    @GetMapping("/batch")
    @PreventDuplicateExecution(
            keys = {},
            useMethodName = true,
            ttl = 10,
            message = "배치 작업이 이미 실행 중입니다."
    )
    public ResponseEntity<String> runBatchProcess() {
        log.info("Starting batch process...");

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("Batch process completed");
        return ResponseEntity.ok("Batch process completed");
    }
}
