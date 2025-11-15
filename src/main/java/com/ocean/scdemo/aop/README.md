# 중복 실행 방지 (Duplicate Execution Prevention)

AOP와 Redis를 활용한 메소드 중복 실행 방지 기능입니다.

## 📋 목차

1. [개요](#개요)
2. [주요 기능](#주요-기능)
3. [의존성](#의존성)
4. [사용법](#사용법)
5. [API 예제](#api-예제)
6. [고급 사용법](#고급-사용법)
7. [주의사항](#주의사항)

---

## 개요

`@PreventDuplicateExecution` 어노테이션을 사용하여 메소드의 중복 실행을 방지합니다.
Redis를 활용하여 분산 환경에서도 동작하며, Controller와 Service 모두에서 사용 가능합니다.

### 동작 방식

1. 메소드 실행 전에 파라미터 객체에서 지정된 속성값들을 추출
2. 추출한 값들을 조합하여 Redis 락 키 생성
3. Redis에 키가 존재하면 `DuplicateExecutionException` 발생
4. 키가 없으면 TTL과 함께 키 저장 후 메소드 실행
5. 메소드 실행 완료 후 자동으로 락 해제

---

## 주요 기능

✅ **다중 키 지원**: 여러 속성값을 조합하여 고유 키 생성
✅ **TTL 설정**: 락 유지 시간을 초 단위로 설정 (기본 5초)
✅ **커스텀 메시지**: 중복 실행 시 사용자 정의 에러 메시지
✅ **자동 락 해제**: 메소드 실행 완료 시 자동으로 락 해제
✅ **레이어 독립적**: Controller, Service 모두 사용 가능
✅ **분산 환경 지원**: Redis 기반으로 멀티 인스턴스 환경에서 동작

---

## 의존성

```gradle
implementation 'org.springframework.boot:spring-boot-starter-aop'
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
```

---

## 사용법

### 1. 기본 사용법

```java
@Service
public class OrderService {

    @PreventDuplicateExecution(keys = {"userId", "orderId"})
    public String createOrder(OrderRequest request) {
        // 주문 처리 로직
        return "Order created";
    }
}
```

### 2. TTL 설정

```java
@PreventDuplicateExecution(
    keys = {"paymentId"},
    ttl = 30  // 30초간 중복 실행 방지
)
public String processPayment(PaymentRequest request) {
    // 결제 처리 로직
    return "Payment processed";
}
```

### 3. 커스텀 메시지

```java
@PreventDuplicateExecution(
    keys = {"userId", "orderId"},
    ttl = 5,
    message = "동일한 주문이 이미 처리 중입니다. 잠시 후 다시 시도해주세요."
)
public String createOrder(OrderRequest request) {
    // 주문 처리 로직
    return "Order created";
}
```

### 4. 메소드명 기반 락 (파라미터 무관)

```java
@PreventDuplicateExecution(
    keys = {},
    useMethodName = true,
    ttl = 10
)
public String runBatchProcess() {
    // 배치 작업 로직
    return "Batch completed";
}
```

---

## API 예제

### Controller에서 사용

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @PostMapping
    @PreventDuplicateExecution(
        keys = {"userId", "orderId"},
        ttl = 5,
        message = "동일한 주문 요청이 이미 처리 중입니다."
    )
    public ResponseEntity<String> createOrder(@RequestBody OrderRequest request) {
        // 주문 처리
        return ResponseEntity.ok("Order created");
    }
}
```

### 테스트 요청

```bash
# 첫 번째 요청 (성공)
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "orderId": "order456",
    "productId": "product789",
    "quantity": 2,
    "amount": 50000.0
  }'

# 즉시 두 번째 요청 (실패 - 429 Too Many Requests)
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "orderId": "order456",
    "productId": "product789",
    "quantity": 2,
    "amount": 50000.0
  }'
```

### 에러 응답

```json
{
  "timestamp": "2025-01-15T10:30:45.123",
  "status": 429,
  "error": "Too Many Requests",
  "message": "동일한 주문 요청이 이미 처리 중입니다.",
  "lockKey": "execution:lock:OrderController:createOrder:user123:order456"
}
```

---

## 고급 사용법

### 1. 여러 속성값 조합

```java
@PreventDuplicateExecution(
    keys = {"userId", "orderId", "productId"},
    ttl = 10
)
public String cancelOrder(OrderRequest request) {
    // userId, orderId, productId 조합으로 고유 키 생성
    return "Order cancelled";
}
```

### 2. Service 레이어에서 사용

```java
@Service
public class PaymentService {

    @PreventDuplicateExecution(keys = {"paymentId", "userId"})
    public String processPayment(PaymentRequest request) {
        // 결제 처리
        return "Payment processed";
    }
}
```

### 3. 다양한 TTL 설정

```java
// 짧은 TTL (2초)
@PreventDuplicateExecution(keys = {"userId"}, ttl = 2)
public String getHistory(PaymentRequest request) {
    return "History fetched";
}

// 긴 TTL (30초)
@PreventDuplicateExecution(keys = {"paymentId"}, ttl = 30)
public String refundPayment(PaymentRequest request) {
    return "Refund processed";
}
```

---

## 주의사항

### 1. 파라미터 요구사항

- `keys` 배열에 지정된 속성명은 파라미터 객체에 반드시 존재해야 합니다
- getter 메소드 또는 필드를 통해 값을 추출합니다
- 속성값이 `null`인 경우 "null" 문자열로 처리됩니다

### 2. 락 해제

- 메소드 실행이 완료되면 자동으로 락이 해제됩니다
- 예외가 발생해도 `finally` 블록에서 락이 해제됩니다
- TTL이 지나면 자동으로 락이 만료됩니다

### 3. 성능 고려사항

- Redis 연결 상태를 확인하세요
- TTL은 메소드 실행 시간보다 길게 설정하는 것을 권장합니다
- 분산 환경에서는 Redis 클러스터 사용을 고려하세요

### 4. 에러 처리

```java
@RestControllerAdvice
public class CustomExceptionHandler {

    @ExceptionHandler(DuplicateExecutionException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateExecution(
        DuplicateExecutionException e
    ) {
        // 중복 실행 에러 처리
        return ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)
            .body(errorResponse);
    }
}
```

---

## 파일 구조

```
com.ocean.scdemo.aop/
├── annotation/
│   └── PreventDuplicateExecution.java    # 어노테이션 정의
├── aspect/
│   └── DuplicateExecutionPreventAspect.java  # AOP 로직
├── exception/
│   └── DuplicateExecutionException.java  # 예외 클래스
└── example/
    ├── dto/
    │   ├── OrderRequest.java
    │   └── PaymentRequest.java
    ├── service/
    │   └── OrderService.java
    └── controller/
        ├── OrderController.java
        └── PaymentController.java
```

---

## 테스트 시나리오

### 1. 정상 실행 테스트

```bash
# 주문 생성 (성공)
curl -X POST http://localhost:8080/api/orders/service \
  -H "Content-Type: application/json" \
  -d '{"userId": "user1", "orderId": "order1", "productId": "prod1", "quantity": 1, "amount": 10000.0}'
```

### 2. 중복 실행 테스트

```bash
# 첫 번째 요청 (성공)
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{"paymentId": "pay1", "userId": "user1", "amount": 10000.0, "paymentMethod": "card"}' &

# 즉시 두 번째 요청 (실패 - 429)
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{"paymentId": "pay1", "userId": "user1", "amount": 10000.0, "paymentMethod": "card"}'
```

### 3. TTL 만료 후 재실행 테스트

```bash
# 첫 번째 요청
curl -X POST http://localhost:8080/api/payments/history \
  -H "Content-Type: application/json" \
  -d '{"userId": "user1", "amount": 0}'

# 2초 대기 후 재요청 (성공)
sleep 3
curl -X POST http://localhost:8080/api/payments/history \
  -H "Content-Type: application/json" \
  -d '{"userId": "user1", "amount": 0}'
```

---

## 라이센스

이 프로젝트는 MIT 라이센스를 따릅니다.
