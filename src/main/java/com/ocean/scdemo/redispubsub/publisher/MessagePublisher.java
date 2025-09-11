package com.ocean.scdemo.redispubsub.publisher;

import com.ocean.scdemo.redispubsub.config.RedisPubSubConfig;
import com.ocean.scdemo.redispubsub.message.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis Pub/Sub 메시지 발행자
 * 
 * 핵심 기능:
 * - 타입별 메시지 발행 (채팅, 알림, 이벤트, 메트릭스, 헬스체크)
 * - 비동기/동기 발행 지원
 * - 배치 발행 최적화
 * - 자동 채널 라우팅
 * - 메시지 검증 및 에러 핸들링
 * - 발행 메트릭스 수집
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessagePublisher {

    private final RedisTemplate<String, Object> redisPubSubTemplate;
    
    // 발행 통계
    private final AtomicLong totalPublished = new AtomicLong(0);
    private final AtomicLong totalFailed = new AtomicLong(0);
    private final AtomicLong chatMessagesPublished = new AtomicLong(0);
    private final AtomicLong notificationMessagesPublished = new AtomicLong(0);
    private final AtomicLong eventMessagesPublished = new AtomicLong(0);
    private final AtomicLong metricsMessagesPublished = new AtomicLong(0);
    private final AtomicLong healthCheckMessagesPublished = new AtomicLong(0);
    
    /**
     * 채팅 메시지 발행
     */
    public boolean publishChatMessage(ChatMessage message) {
        try {
            validateMessage(message);
            enrichChatMessage(message);
            
            String channel = determineChatChannel(message);
            message.setChannel(channel);
            
            Long result = redisPubSubTemplate.convertAndSend(channel, message);
            
            if (result != null && result > 0) {
                chatMessagesPublished.incrementAndGet();
                totalPublished.incrementAndGet();
                log.debug("채팅 메시지 발행 성공: 채널={}, 메시지ID={}, 수신자={}", 
                         channel, message.getMessageId(), result);
                return true;
            } else {
                log.warn("채팅 메시지 발행 실패 - 수신자 없음: {}", channel);
                return false;
            }
            
        } catch (Exception e) {
            totalFailed.incrementAndGet();
            log.error("채팅 메시지 발행 실패: {}", message.getMessageId(), e);
            return false;
        }
    }
    
    /**
     * 알림 메시지 발행
     */
    public boolean publishNotification(NotificationMessage message) {
        try {
            validateMessage(message);
            enrichNotificationMessage(message);
            
            String channel = determineNotificationChannel(message);
            message.setChannel(channel);
            
            Long result = redisPubSubTemplate.convertAndSend(channel, message);
            
            if (result != null && result > 0) {
                notificationMessagesPublished.incrementAndGet();
                totalPublished.incrementAndGet();
                log.debug("알림 메시지 발행 성공: 채널={}, 메시지ID={}, 수신자={}", 
                         channel, message.getMessageId(), result);
                return true;
            } else {
                log.warn("알림 메시지 발행 실패 - 수신자 없음: {}", channel);
                return false;
            }
            
        } catch (Exception e) {
            totalFailed.incrementAndGet();
            log.error("알림 메시지 발행 실패: {}", message.getMessageId(), e);
            return false;
        }
    }
    
    /**
     * 시스템 이벤트 메시지 발행
     */
    public boolean publishSystemEvent(SystemEventMessage message) {
        try {
            validateMessage(message);
            enrichSystemEventMessage(message);
            
            String channel = determineSystemEventChannel(message);
            message.setChannel(channel);
            
            Long result = redisPubSubTemplate.convertAndSend(channel, message);
            
            if (result != null && result > 0) {
                eventMessagesPublished.incrementAndGet();
                totalPublished.incrementAndGet();
                log.debug("시스템 이벤트 발행 성공: 채널={}, 이벤트={}, 수신자={}", 
                         channel, message.getEventType(), result);
                return true;
            } else {
                log.warn("시스템 이벤트 발행 실패 - 수신자 없음: {}", channel);
                return false;
            }
            
        } catch (Exception e) {
            totalFailed.incrementAndGet();
            log.error("시스템 이벤트 발행 실패: {}", message.getMessageId(), e);
            return false;
        }
    }
    
    /**
     * 사용자 이벤트 메시지 발행
     */
    public boolean publishUserEvent(UserEventMessage message) {
        try {
            validateMessage(message);
            enrichUserEventMessage(message);
            
            String channel = determineUserEventChannel(message);
            message.setChannel(channel);
            
            Long result = redisPubSubTemplate.convertAndSend(channel, message);
            
            if (result != null && result > 0) {
                eventMessagesPublished.incrementAndGet();
                totalPublished.incrementAndGet();
                log.debug("사용자 이벤트 발행 성공: 채널={}, 이벤트={}, 사용자={}, 수신자={}", 
                         channel, message.getEventType(), message.getUserId(), result);
                return true;
            } else {
                log.warn("사용자 이벤트 발행 실패 - 수신자 없음: {}", channel);
                return false;
            }
            
        } catch (Exception e) {
            totalFailed.incrementAndGet();
            log.error("사용자 이벤트 발행 실패: {}", message.getMessageId(), e);
            return false;
        }
    }
    
    /**
     * 메트릭스 메시지 발행
     */
    public boolean publishMetrics(MetricsMessage message) {
        try {
            validateMessage(message);
            enrichMetricsMessage(message);
            
            String channel = determineMetricsChannel(message);
            message.setChannel(channel);
            
            Long result = redisPubSubTemplate.convertAndSend(channel, message);
            
            if (result != null && result > 0) {
                metricsMessagesPublished.incrementAndGet();
                totalPublished.incrementAndGet();
                log.debug("메트릭스 발행 성공: 채널={}, 메트릭={}, 수신자={}", 
                         channel, message.getMetricName(), result);
                return true;
            } else {
                log.warn("메트릭스 발행 실패 - 수신자 없음: {}", channel);
                return false;
            }
            
        } catch (Exception e) {
            totalFailed.incrementAndGet();
            log.error("메트릭스 발행 실패: {}", message.getMessageId(), e);
            return false;
        }
    }
    
    /**
     * 헬스체크 메시지 발행
     */
    public boolean publishHealthCheck(HealthCheckMessage message) {
        try {
            validateMessage(message);
            enrichHealthCheckMessage(message);
            
            String channel = determineHealthCheckChannel(message);
            message.setChannel(channel);
            
            Long result = redisPubSubTemplate.convertAndSend(channel, message);
            
            if (result != null && result > 0) {
                healthCheckMessagesPublished.incrementAndGet();
                totalPublished.incrementAndGet();
                log.debug("헬스체크 발행 성공: 채널={}, 서비스={}, 상태={}, 수신자={}", 
                         channel, message.getServiceName(), message.getStatus(), result);
                return true;
            } else {
                log.warn("헬스체크 발행 실패 - 수신자 없음: {}", channel);
                return false;
            }
            
        } catch (Exception e) {
            totalFailed.incrementAndGet();
            log.error("헬스체크 발행 실패: {}", message.getMessageId(), e);
            return false;
        }
    }
    
    /**
     * 비동기 메시지 발행
     */
    public CompletableFuture<Boolean> publishAsync(BaseMessage message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return switch (message) {
                    case ChatMessage chatMsg -> publishChatMessage(chatMsg);
                    case NotificationMessage notificationMsg -> publishNotification(notificationMsg);
                    case SystemEventMessage systemEventMsg -> publishSystemEvent(systemEventMsg);
                    case UserEventMessage userEventMsg -> publishUserEvent(userEventMsg);
                    case MetricsMessage metricsMsg -> publishMetrics(metricsMsg);
                    case HealthCheckMessage healthMsg -> publishHealthCheck(healthMsg);
                    default -> {
                        log.warn("지원하지 않는 메시지 타입: {}", message.getClass().getSimpleName());
                        yield false;
                    }
                };
            } catch (Exception e) {
                log.error("비동기 메시지 발행 실패: {}", message.getMessageId(), e);
                return false;
            }
        });
    }
    
    /**
     * 배치 메시지 발행 (동일한 채널)
     */
    public BatchPublishResult publishBatch(String channel, List<BaseMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return new BatchPublishResult(0, 0, "메시지 목록이 비어있습니다");
        }
        
        int successCount = 0;
        int failureCount = 0;
        StringBuilder errors = new StringBuilder();
        
        for (BaseMessage message : messages) {
            try {
                validateMessage(message);
                message.setChannel(channel);
                
                Long result = redisPubSubTemplate.convertAndSend(channel, message);
                
                if (result != null && result > 0) {
                    successCount++;
                    totalPublished.incrementAndGet();
                } else {
                    failureCount++;
                    errors.append(String.format("메시지 %s: 수신자 없음; ", message.getMessageId()));
                }
                
            } catch (Exception e) {
                failureCount++;
                totalFailed.incrementAndGet();
                errors.append(String.format("메시지 %s: %s; ", message.getMessageId(), e.getMessage()));
            }
        }
        
        log.info("배치 발행 완료: 채널={}, 성공={}, 실패={}, 전체={}", 
                channel, successCount, failureCount, messages.size());
        
        return new BatchPublishResult(successCount, failureCount, errors.toString());
    }
    
    /**
     * 브로드캐스트 메시지 발행 (모든 구독자에게)
     */
    public boolean publishBroadcast(BaseMessage message) {
        try {
            validateMessage(message);
            message.setChannel(RedisPubSubConfig.Channels.NOTIFICATION_BROADCAST);
            
            Long result = redisPubSubTemplate.convertAndSend(
                RedisPubSubConfig.Channels.NOTIFICATION_BROADCAST, 
                message
            );
            
            if (result != null && result > 0) {
                totalPublished.incrementAndGet();
                log.info("브로드캐스트 메시지 발행 성공: 메시지ID={}, 수신자={}", 
                        message.getMessageId(), result);
                return true;
            } else {
                log.warn("브로드캐스트 메시지 발행 실패 - 수신자 없음");
                return false;
            }
            
        } catch (Exception e) {
            totalFailed.incrementAndGet();
            log.error("브로드캐스트 메시지 발행 실패: {}", message.getMessageId(), e);
            return false;
        }
    }
    
    /**
     * 발행 통계 조회
     */
    public PublishStats getPublishStats() {
        return PublishStats.builder()
                .totalPublished(totalPublished.get())
                .totalFailed(totalFailed.get())
                .chatMessages(chatMessagesPublished.get())
                .notificationMessages(notificationMessagesPublished.get())
                .eventMessages(eventMessagesPublished.get())
                .metricsMessages(metricsMessagesPublished.get())
                .healthCheckMessages(healthCheckMessagesPublished.get())
                .successRate(calculateSuccessRate())
                .build();
    }
    
    /**
     * 통계 초기화
     */
    public void resetStats() {
        totalPublished.set(0);
        totalFailed.set(0);
        chatMessagesPublished.set(0);
        notificationMessagesPublished.set(0);
        eventMessagesPublished.set(0);
        metricsMessagesPublished.set(0);
        healthCheckMessagesPublished.set(0);
        log.info("발행 통계가 초기화되었습니다");
    }
    
    // === 내부 메서드들 ===
    
    /**
     * 메시지 검증
     */
    private void validateMessage(BaseMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("메시지가 null입니다");
        }
        if (!StringUtils.hasText(message.getMessageId())) {
            throw new IllegalArgumentException("메시지 ID가 필요합니다");
        }
        if (!StringUtils.hasText(message.getMessageType())) {
            throw new IllegalArgumentException("메시지 타입이 필요합니다");
        }
        if (message.isExpired()) {
            throw new IllegalArgumentException("만료된 메시지입니다: " + message.getMessageId());
        }
    }
    
    /**
     * 채팅 메시지 채널 결정
     */
    private String determineChatChannel(ChatMessage message) {
        if (StringUtils.hasText(message.getRoomId())) {
            return RedisPubSubConfig.Channels.chatRoom(message.getRoomId());
        }
        return RedisPubSubConfig.Channels.CHAT_GLOBAL;
    }
    
    /**
     * 알림 메시지 채널 결정
     */
    private String determineNotificationChannel(NotificationMessage message) {
        if (StringUtils.hasText(message.getRecipientId())) {
            return RedisPubSubConfig.Channels.userNotification(message.getRecipientId());
        }
        if ("SYSTEM".equals(message.getNotificationType())) {
            return RedisPubSubConfig.Channels.NOTIFICATION_SYSTEM;
        }
        return RedisPubSubConfig.Channels.NOTIFICATION_BROADCAST;
    }
    
    /**
     * 시스템 이벤트 채널 결정
     */
    private String determineSystemEventChannel(SystemEventMessage message) {
        if (message.isErrorEvent()) {
            return RedisPubSubConfig.Channels.EVENT_SYSTEM + ":errors";
        }
        return RedisPubSubConfig.Channels.EVENT_SYSTEM;
    }
    
    /**
     * 사용자 이벤트 채널 결정
     */
    private String determineUserEventChannel(UserEventMessage message) {
        if (StringUtils.hasText(message.getUserId())) {
            return RedisPubSubConfig.Channels.userEvent(message.getUserId());
        }
        return RedisPubSubConfig.Channels.EVENT_SYSTEM + ":user_events";
    }
    
    /**
     * 메트릭스 채널 결정
     */
    private String determineMetricsChannel(MetricsMessage message) {
        if (message.isPerformanceMetric()) {
            return RedisPubSubConfig.Channels.METRICS_PERFORMANCE;
        }
        return RedisPubSubConfig.Channels.METRICS_USAGE;
    }
    
    /**
     * 헬스체크 채널 결정
     */
    private String determineHealthCheckChannel(HealthCheckMessage message) {
        if (message.isDown() || message.isDegraded()) {
            return RedisPubSubConfig.Channels.HEALTH_STATUS + ":alerts";
        }
        return RedisPubSubConfig.Channels.HEALTH_STATUS;
    }
    
    /**
     * 채팅 메시지 보강
     */
    private void enrichChatMessage(ChatMessage message) {
        if (!StringUtils.hasText(message.getStatus())) {
            message.setStatus("SENT");
        }
        message.addMetadata("publishTime", LocalDateTime.now());
    }
    
    /**
     * 알림 메시지 보강
     */
    private void enrichNotificationMessage(NotificationMessage message) {
        if (!StringUtils.hasText(message.getStatus())) {
            message.setStatus("PENDING");
        }
        message.addMetadata("publishTime", LocalDateTime.now());
    }
    
    /**
     * 시스템 이벤트 메시지 보강
     */
    private void enrichSystemEventMessage(SystemEventMessage message) {
        if (!StringUtils.hasText(message.getInstanceId())) {
            message.setInstanceId(System.getProperty("instance.id", "unknown"));
        }
        if (!StringUtils.hasText(message.getHostName())) {
            message.setHostName(System.getProperty("hostname", "unknown"));
        }
        message.addEventData("publishTime", LocalDateTime.now());
    }
    
    /**
     * 사용자 이벤트 메시지 보강
     */
    private void enrichUserEventMessage(UserEventMessage message) {
        message.addEventProperty("publishTime", LocalDateTime.now());
    }
    
    /**
     * 메트릭스 메시지 보강
     */
    private void enrichMetricsMessage(MetricsMessage message) {
        if (message.getMeasurementTime() == null) {
            message.setMeasurementTime(LocalDateTime.now());
        }
        message.addLabel("publishTime", LocalDateTime.now().toString());
    }
    
    /**
     * 헬스체크 메시지 보강
     */
    private void enrichHealthCheckMessage(HealthCheckMessage message) {
        message.addAdditionalInfo("publishTime", LocalDateTime.now());
    }
    
    /**
     * 성공률 계산
     */
    private double calculateSuccessRate() {
        long total = totalPublished.get() + totalFailed.get();
        if (total == 0) return 0.0;
        return ((double) totalPublished.get() / total) * 100.0;
    }
    
    // === DTO 클래스들 ===
    
    /**
     * 배치 발행 결과
     */
    @lombok.Builder
    @lombok.Data
    public static class BatchPublishResult {
        private final int successCount;
        private final int failureCount;
        private final String errors;
        
        public int getTotalCount() {
            return successCount + failureCount;
        }
        
        public double getSuccessRate() {
            int total = getTotalCount();
            return total > 0 ? ((double) successCount / total) * 100.0 : 0.0;
        }
    }
    
    /**
     * 발행 통계
     */
    @lombok.Builder
    @lombok.Data
    public static class PublishStats {
        private final long totalPublished;
        private final long totalFailed;
        private final long chatMessages;
        private final long notificationMessages;
        private final long eventMessages;
        private final long metricsMessages;
        private final long healthCheckMessages;
        private final double successRate;
        
        public long getTotalMessages() {
            return totalPublished + totalFailed;
        }
    }
}