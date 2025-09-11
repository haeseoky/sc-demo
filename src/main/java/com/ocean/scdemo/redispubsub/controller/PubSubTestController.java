package com.ocean.scdemo.redispubsub.controller;

import com.ocean.scdemo.redispubsub.config.RedisSubscriptionConfig;
import com.ocean.scdemo.redispubsub.message.*;
import com.ocean.scdemo.redispubsub.publisher.MessagePublisher;
import com.ocean.scdemo.redispubsub.subscriber.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Redis Pub/Sub 테스트 컨트롤러
 * 
 * 기능:
 * - 다양한 메시지 타입 발행 테스트
 * - 구독 상태 모니터링
 * - 시스템 성능 및 통계 조회
 * - 실시간 메시지 시뮬레이션
 */
@Slf4j
@Tag(name = "Redis Pub/Sub Test API", description = "Redis Pub/Sub 시스템 테스트 및 모니터링 API")
@RestController
@RequestMapping("/api/pubsub")
@RequiredArgsConstructor
public class PubSubTestController {

    private final MessagePublisher messagePublisher;
    private final MessageSubscriber messageSubscriber;
    private final ChatMessageHandler chatMessageHandler;
    private final NotificationMessageHandler notificationMessageHandler;
    private final SystemEventMessageHandler systemEventMessageHandler;
    private final UserEventMessageHandler userEventMessageHandler;
    private final MetricsMessageHandler metricsMessageHandler;
    private final HealthCheckMessageHandler healthCheckMessageHandler;
    private final RedisSubscriptionConfig.SubscriptionManager subscriptionManager;

    /**
     * 채팅 메시지 발행 테스트
     */
    @Operation(summary = "채팅 메시지 발행", description = "테스트용 채팅 메시지를 발행합니다")
    @PostMapping("/test/chat")
    public ResponseEntity<?> publishChatMessage(
            @Parameter(description = "발신자 ID") @RequestParam String senderId,
            @Parameter(description = "발신자 이름") @RequestParam String senderName,
            @Parameter(description = "채팅방 ID") @RequestParam(required = false) String roomId,
            @Parameter(description = "메시지 내용") @RequestParam String content) {
        
        try {
            ChatMessage chatMessage = ChatMessage.createTextMessage(
                senderId, senderName, 
                roomId != null ? roomId : "global", 
                content
            );
            
            boolean success = messagePublisher.publishChatMessage(chatMessage);
            
            return ResponseEntity.ok(Map.of(
                "success", success,
                "messageId", chatMessage.getMessageId(),
                "message", "채팅 메시지가 발행되었습니다",
                "timestamp", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            log.error("채팅 메시지 발행 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * 알림 메시지 발행 테스트
     */
    @Operation(summary = "알림 메시지 발행", description = "테스트용 알림 메시지를 발행합니다")
    @PostMapping("/test/notification")
    public ResponseEntity<?> publishNotification(
            @Parameter(description = "수신자 ID") @RequestParam String recipientId,
            @Parameter(description = "알림 제목") @RequestParam String title,
            @Parameter(description = "알림 메시지") @RequestParam String message,
            @Parameter(description = "알림 타입") @RequestParam(defaultValue = "PUSH") String type) {
        
        try {
            NotificationMessage notification = NotificationMessage.createPushNotification(
                recipientId, title, message, null
            );
            notification.setNotificationType(type);
            
            boolean success = messagePublisher.publishNotification(notification);
            
            return ResponseEntity.ok(Map.of(
                "success", success,
                "messageId", notification.getMessageId(),
                "message", "알림이 발행되었습니다",
                "timestamp", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            log.error("알림 발행 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * 시스템 이벤트 발행 테스트
     */
    @Operation(summary = "시스템 이벤트 발행", description = "테스트용 시스템 이벤트를 발행합니다")
    @PostMapping("/test/system-event")
    public ResponseEntity<?> publishSystemEvent(
            @Parameter(description = "서비스 이름") @RequestParam String serviceName,
            @Parameter(description = "이벤트 타입") @RequestParam String eventType,
            @Parameter(description = "이벤트 레벨") @RequestParam(defaultValue = "INFO") String eventLevel,
            @Parameter(description = "메시지") @RequestParam String message) {
        
        try {
            SystemEventMessage systemEvent = SystemEventMessage.builder()
                    .messageType("SYSTEM_EVENT")
                    .eventType(eventType)
                    .eventSource(serviceName)
                    .eventLevel(eventLevel)
                    .serviceName(serviceName)
                    .eventName("테스트 이벤트")
                    .description(message)
                    .status("COMPLETED")
                    .eventStartTime(LocalDateTime.now())
                    .build();
            
            boolean success = messagePublisher.publishSystemEvent(systemEvent);
            
            return ResponseEntity.ok(Map.of(
                "success", success,
                "messageId", systemEvent.getMessageId(),
                "message", "시스템 이벤트가 발행되었습니다",
                "timestamp", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            log.error("시스템 이벤트 발행 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * 사용자 이벤트 발행 테스트
     */
    @Operation(summary = "사용자 이벤트 발행", description = "테스트용 사용자 이벤트를 발행합니다")
    @PostMapping("/test/user-event")
    public ResponseEntity<?> publishUserEvent(
            @Parameter(description = "사용자 ID") @RequestParam String userId,
            @Parameter(description = "이벤트 타입") @RequestParam String eventType,
            @Parameter(description = "페이지 URL") @RequestParam(required = false) String pageUrl,
            @Parameter(description = "디바이스 타입") @RequestParam(defaultValue = "WEB") String deviceType) {
        
        try {
            UserEventMessage userEvent = UserEventMessage.builder()
                    .messageType("USER_EVENT")
                    .eventType(eventType)
                    .userId(userId)
                    .userName("testUser_" + userId)
                    .eventName("테스트 사용자 이벤트")
                    .description("사용자가 " + eventType + " 이벤트를 실행했습니다")
                    .category("USER_ACTION")
                    .deviceType(deviceType)
                    .pageUrl(pageUrl)
                    .sessionId(UUID.randomUUID().toString())
                    .status("SUCCESS")
                    .build();
            
            boolean success = messagePublisher.publishUserEvent(userEvent);
            
            return ResponseEntity.ok(Map.of(
                "success", success,
                "messageId", userEvent.getMessageId(),
                "message", "사용자 이벤트가 발행되었습니다",
                "timestamp", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            log.error("사용자 이벤트 발행 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * 메트릭스 발행 테스트
     */
    @Operation(summary = "메트릭스 발행", description = "테스트용 메트릭스를 발행합니다")
    @PostMapping("/test/metrics")
    public ResponseEntity<?> publishMetrics(
            @Parameter(description = "메트릭 이름") @RequestParam String metricName,
            @Parameter(description = "메트릭 값") @RequestParam Double value,
            @Parameter(description = "메트릭 타입") @RequestParam(defaultValue = "CUSTOM") String metricType,
            @Parameter(description = "단위") @RequestParam(defaultValue = "COUNT") String unit) {
        
        try {
            MetricsMessage metrics = MetricsMessage.builder()
                    .messageType("METRICS")
                    .metricType(metricType)
                    .metricName(metricName)
                    .value(value)
                    .unit(unit)
                    .source("test-service")
                    .measurementTime(LocalDateTime.now())
                    .environment("TEST")
                    .build();
            
            boolean success = messagePublisher.publishMetrics(metrics);
            
            return ResponseEntity.ok(Map.of(
                "success", success,
                "messageId", metrics.getMessageId(),
                "message", "메트릭스가 발행되었습니다",
                "timestamp", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            log.error("메트릭스 발행 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * 헬스체크 발행 테스트
     */
    @Operation(summary = "헬스체크 발행", description = "테스트용 헬스체크 메시지를 발행합니다")
    @PostMapping("/test/health-check")
    public ResponseEntity<?> publishHealthCheck(
            @Parameter(description = "서비스 이름") @RequestParam String serviceName,
            @Parameter(description = "상태") @RequestParam(defaultValue = "UP") String status,
            @Parameter(description = "응답시간") @RequestParam(required = false) Long responseTime) {
        
        try {
            HealthCheckMessage healthCheck = HealthCheckMessage.createServiceHealthCheck(
                serviceName, status, responseTime
            );
            
            boolean success = messagePublisher.publishHealthCheck(healthCheck);
            
            return ResponseEntity.ok(Map.of(
                "success", success,
                "messageId", healthCheck.getMessageId(),
                "message", "헬스체크가 발행되었습니다",
                "timestamp", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            log.error("헬스체크 발행 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * 비동기 메시지 발행 테스트
     */
    @Operation(summary = "비동기 메시지 발행", description = "비동기로 메시지를 발행합니다")
    @PostMapping("/test/async")
    public ResponseEntity<?> publishAsyncMessage(@RequestParam String messageType) {
        try {
            BaseMessage message = createTestMessage(messageType);
            
            CompletableFuture<Boolean> future = messagePublisher.publishAsync(message);
            
            // 논블로킹으로 결과 처리
            future.thenAccept(success -> {
                log.info("비동기 발행 완료: 메시지ID={}, 성공={}", message.getMessageId(), success);
            });
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "messageId", message.getMessageId(),
                "message", "비동기 발행 요청이 처리되었습니다",
                "timestamp", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            log.error("비동기 메시지 발행 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * 브로드캐스트 메시지 발행
     */
    @Operation(summary = "브로드캐스트 발행", description = "모든 구독자에게 메시지를 브로드캐스트합니다")
    @PostMapping("/test/broadcast")
    public ResponseEntity<?> publishBroadcast(@RequestParam String message) {
        try {
            NotificationMessage broadcast = NotificationMessage.createSystemAlert(
                "시스템 공지", message, "MEDIUM"
            );
            
            boolean success = messagePublisher.publishBroadcast(broadcast);
            
            return ResponseEntity.ok(Map.of(
                "success", success,
                "messageId", broadcast.getMessageId(),
                "message", "브로드캐스트가 발행되었습니다",
                "timestamp", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            log.error("브로드캐스트 발행 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * 발행 통계 조회
     */
    @Operation(summary = "발행 통계 조회", description = "메시지 발행 통계를 조회합니다")
    @GetMapping("/stats/publisher")
    public ResponseEntity<MessagePublisher.PublishStats> getPublisherStats() {
        return ResponseEntity.ok(messagePublisher.getPublishStats());
    }

    /**
     * 구독 통계 조회
     */
    @Operation(summary = "구독 통계 조회", description = "메시지 구독 통계를 조회합니다")
    @GetMapping("/stats/subscriber")
    public ResponseEntity<MessageSubscriber.SubscriptionStats> getSubscriberStats() {
        return ResponseEntity.ok(messageSubscriber.getSubscriptionStats());
    }

    /**
     * 핸들러별 통계 조회
     */
    @Operation(summary = "핸들러 통계 조회", description = "각 메시지 핸들러의 처리 통계를 조회합니다")
    @GetMapping("/stats/handlers")
    public ResponseEntity<Map<String, Object>> getHandlerStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("chat", chatMessageHandler.getStats());
        stats.put("notification", notificationMessageHandler.getStats());
        stats.put("systemEvent", systemEventMessageHandler.getStats());
        stats.put("userEvent", userEventMessageHandler.getStats());
        stats.put("metrics", metricsMessageHandler.getStats());
        stats.put("healthCheck", healthCheckMessageHandler.getStats());
        
        return ResponseEntity.ok(stats);
    }

    /**
     * 구독 관리 상태 조회
     */
    @Operation(summary = "구독 상태 조회", description = "Redis 구독 관리 상태를 조회합니다")
    @GetMapping("/stats/subscription")
    public ResponseEntity<RedisSubscriptionConfig.SubscriptionStats> getSubscriptionStats() {
        return ResponseEntity.ok(subscriptionManager.getSubscriptionStats());
    }

    /**
     * 시스템 상태 종합 조회
     */
    @Operation(summary = "시스템 상태 조회", description = "Redis Pub/Sub 시스템 전체 상태를 조회합니다")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // 발행자 상태
        MessagePublisher.PublishStats publishStats = messagePublisher.getPublishStats();
        status.put("publisher", Map.of(
            "healthy", publishStats.getSuccessRate() > 80.0,
            "successRate", publishStats.getSuccessRate(),
            "totalMessages", publishStats.getTotalMessages()
        ));
        
        // 구독자 상태
        MessageSubscriber.SubscriptionStats subscriptionStats = messageSubscriber.getSubscriptionStats();
        status.put("subscriber", Map.of(
            "healthy", subscriptionStats.getSuccessRate() > 80.0,
            "successRate", subscriptionStats.getSuccessRate(),
            "totalReceived", subscriptionStats.getTotalReceived()
        ));
        
        // 구독 관리 상태
        boolean subscriptionHealthy = subscriptionManager.isHealthy();
        status.put("subscriptionManager", Map.of(
            "healthy", subscriptionHealthy
        ));
        
        // 전체 시스템 상태
        boolean systemHealthy = publishStats.getSuccessRate() > 80.0 && 
                               subscriptionStats.getSuccessRate() > 80.0 && 
                               subscriptionHealthy;
        
        status.put("overall", Map.of(
            "healthy", systemHealthy,
            "status", systemHealthy ? "HEALTHY" : "DEGRADED",
            "timestamp", LocalDateTime.now()
        ));
        
        return ResponseEntity.ok(status);
    }

    /**
     * 통계 초기화
     */
    @Operation(summary = "통계 초기화", description = "모든 통계를 초기화합니다")
    @PostMapping("/reset-stats")
    public ResponseEntity<Map<String, String>> resetStats() {
        try {
            messagePublisher.resetStats();
            messageSubscriber.resetStats();
            
            return ResponseEntity.ok(Map.of(
                "message", "통계가 초기화되었습니다",
                "timestamp", LocalDateTime.now().toString()
            ));
            
        } catch (Exception e) {
            log.error("통계 초기화 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }

    // === 헬퍼 메서드 ===

    private BaseMessage createTestMessage(String messageType) {
        return switch (messageType.toUpperCase()) {
            case "CHAT" -> ChatMessage.createTextMessage("test", "TestUser", "global", "테스트 메시지");
            case "NOTIFICATION" -> NotificationMessage.createInfoNotification("test", "테스트", "테스트 알림");
            case "SYSTEM_EVENT" -> SystemEventMessage.createServerStartEvent("test-service", "test-instance");
            case "USER_EVENT" -> UserEventMessage.createLoginEvent("test", "TestUser", "session123", "WEB");
            case "METRICS" -> MetricsMessage.createPerformanceMetric("test-service", "test_metric", 100.0, "COUNT");
            case "HEALTH_CHECK" -> HealthCheckMessage.createServiceHealthCheck("test-service", "UP", 50L);
            default -> throw new IllegalArgumentException("지원하지 않는 메시지 타입: " + messageType);
        };
    }
}