package com.ocean.scdemo.redispubsub.subscriber;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocean.scdemo.redispubsub.message.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis Pub/Sub 메시지 구독자
 * 
 * 핵심 기능:
 * - 모든 채널의 메시지 통합 처리
 * - 메시지 타입별 자동 라우팅
 * - JSON 역직렬화 및 타입 안전성 보장
 * - 에러 핸들링 및 복구
 * - 처리 통계 수집
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageSubscriber implements MessageListener {

    private final ChatMessageHandler chatMessageHandler;
    private final NotificationMessageHandler notificationMessageHandler;
    private final SystemEventMessageHandler systemEventMessageHandler;
    private final UserEventMessageHandler userEventMessageHandler;
    private final MetricsMessageHandler metricsMessageHandler;
    private final HealthCheckMessageHandler healthCheckMessageHandler;
    private final ObjectMapper objectMapper;
    
    // 처리 통계
    private final AtomicLong totalReceived = new AtomicLong(0);
    private final AtomicLong totalProcessed = new AtomicLong(0);
    private final AtomicLong totalFailed = new AtomicLong(0);
    private final AtomicLong totalIgnored = new AtomicLong(0);

    @Override
    public void onMessage(Message message, byte[] pattern) {
        totalReceived.incrementAndGet();
        
        try {
            String channel = new String(message.getChannel());
            String body = new String(message.getBody());
            
            log.debug("메시지 수신: 채널={}, 크기={}bytes", channel, body.length());
            
            // 메시지가 비어있는지 확인
            if (body == null || body.trim().isEmpty()) {
                log.warn("빈 메시지 수신: 채널={}", channel);
                totalIgnored.incrementAndGet();
                return;
            }
            
            // JSON을 BaseMessage로 역직렬화
            BaseMessage baseMessage = deserializeMessage(body);
            
            if (baseMessage == null) {
                log.warn("메시지 역직렬화 실패: 채널={}, 내용={}", channel, 
                        body.length() > 200 ? body.substring(0, 200) + "..." : body);
                totalFailed.incrementAndGet();
                return;
            }
            
            // 메시지 검증
            if (!isValidMessage(baseMessage)) {
                log.warn("유효하지 않은 메시지: ID={}, 타입={}", 
                        baseMessage.getMessageId(), baseMessage.getMessageType());
                totalIgnored.incrementAndGet();
                return;
            }
            
            // 채널 정보 설정
            baseMessage.setChannel(channel);
            
            // 메시지 타입에 따라 적절한 핸들러로 라우팅
            boolean processed = routeMessage(baseMessage);
            
            if (processed) {
                totalProcessed.incrementAndGet();
                log.debug("메시지 처리 완료: ID={}, 타입={}, 채널={}", 
                         baseMessage.getMessageId(), baseMessage.getMessageType(), channel);
            } else {
                totalFailed.incrementAndGet();
                log.warn("메시지 처리 실패: ID={}, 타입={}", 
                        baseMessage.getMessageId(), baseMessage.getMessageType());
            }
            
        } catch (Exception e) {
            totalFailed.incrementAndGet();
            log.error("메시지 처리 중 예외 발생: 채널={}", 
                     message.getChannel() != null ? new String(message.getChannel()) : "unknown", e);
        }
    }
    
    /**
     * 메시지 타입별 라우팅
     */
    private boolean routeMessage(BaseMessage message) {
        try {
            return switch (message) {
                case ChatMessage chatMsg -> {
                    log.debug("채팅 메시지 처리: ID={}, 방ID={}", chatMsg.getMessageId(), chatMsg.getRoomId());
                    yield chatMessageHandler.handleMessage(chatMsg);
                }
                case NotificationMessage notificationMsg -> {
                    log.debug("알림 메시지 처리: ID={}, 타입={}", notificationMsg.getMessageId(), notificationMsg.getNotificationType());
                    yield notificationMessageHandler.handleMessage(notificationMsg);
                }
                case SystemEventMessage systemEventMsg -> {
                    log.debug("시스템 이벤트 처리: ID={}, 이벤트={}", systemEventMsg.getMessageId(), systemEventMsg.getEventType());
                    yield systemEventMessageHandler.handleMessage(systemEventMsg);
                }
                case UserEventMessage userEventMsg -> {
                    log.debug("사용자 이벤트 처리: ID={}, 사용자={}, 이벤트={}", 
                             userEventMsg.getMessageId(), userEventMsg.getUserId(), userEventMsg.getEventType());
                    yield userEventMessageHandler.handleMessage(userEventMsg);
                }
                case MetricsMessage metricsMsg -> {
                    log.debug("메트릭스 처리: ID={}, 메트릭={}", metricsMsg.getMessageId(), metricsMsg.getMetricName());
                    yield metricsMessageHandler.handleMessage(metricsMsg);
                }
                case HealthCheckMessage healthMsg -> {
                    log.debug("헬스체크 처리: ID={}, 서비스={}, 상태={}", 
                             healthMsg.getMessageId(), healthMsg.getServiceName(), healthMsg.getStatus());
                    yield healthCheckMessageHandler.handleMessage(healthMsg);
                }
                default -> {
                    log.warn("지원하지 않는 메시지 타입: {}, ID={}", 
                            message.getClass().getSimpleName(), message.getMessageId());
                    yield false;
                }
            };
        } catch (Exception e) {
            log.error("메시지 라우팅 중 오류 발생: ID={}, 타입={}", 
                     message.getMessageId(), message.getMessageType(), e);
            return false;
        }
    }
    
    /**
     * JSON 문자열을 BaseMessage 객체로 역직렬화
     */
    private BaseMessage deserializeMessage(String jsonMessage) {
        try {
            // Jackson의 다형성 역직렬화를 사용하여 자동으로 올바른 타입으로 변환
            return objectMapper.readValue(jsonMessage, BaseMessage.class);
        } catch (Exception e) {
            log.error("메시지 역직렬화 실패: {}", jsonMessage.length() > 500 ? 
                     jsonMessage.substring(0, 500) + "..." : jsonMessage, e);
            return null;
        }
    }
    
    /**
     * 메시지 유효성 검증
     */
    private boolean isValidMessage(BaseMessage message) {
        if (message == null) {
            return false;
        }
        
        // 기본 필드 검증
        if (message.getMessageId() == null || message.getMessageId().trim().isEmpty()) {
            log.warn("메시지 ID가 없음");
            return false;
        }
        
        if (message.getMessageType() == null || message.getMessageType().trim().isEmpty()) {
            log.warn("메시지 타입이 없음: ID={}", message.getMessageId());
            return false;
        }
        
        // TTL 검증 (만료된 메시지 거부)
        if (message.isExpired()) {
            log.warn("만료된 메시지: ID={}, TTL={}, 생성시간={}", 
                    message.getMessageId(), message.getTtl(), message.getTimestamp());
            return false;
        }
        
        // 메시지별 추가 검증
        return isValidSpecificMessage(message);
    }
    
    /**
     * 메시지 타입별 상세 검증
     */
    private boolean isValidSpecificMessage(BaseMessage message) {
        try {
            return switch (message) {
                case ChatMessage chatMsg -> validateChatMessage(chatMsg);
                case NotificationMessage notificationMsg -> validateNotificationMessage(notificationMsg);
                case SystemEventMessage systemEventMsg -> validateSystemEventMessage(systemEventMsg);
                case UserEventMessage userEventMsg -> validateUserEventMessage(userEventMsg);
                case MetricsMessage metricsMsg -> validateMetricsMessage(metricsMsg);
                case HealthCheckMessage healthMsg -> validateHealthCheckMessage(healthMsg);
                default -> true; // 알 수 없는 타입은 통과
            };
        } catch (Exception e) {
            log.error("메시지 검증 중 오류: ID={}", message.getMessageId(), e);
            return false;
        }
    }
    
    private boolean validateChatMessage(ChatMessage message) {
        return message.getContent() != null && !message.getContent().trim().isEmpty();
    }
    
    private boolean validateNotificationMessage(NotificationMessage message) {
        return message.getTitle() != null && message.getMessage() != null;
    }
    
    private boolean validateSystemEventMessage(SystemEventMessage message) {
        return message.getEventType() != null && message.getServiceName() != null;
    }
    
    private boolean validateUserEventMessage(UserEventMessage message) {
        return message.getEventType() != null && message.getUserId() != null;
    }
    
    private boolean validateMetricsMessage(MetricsMessage message) {
        return message.getMetricName() != null && message.getValue() != null;
    }
    
    private boolean validateHealthCheckMessage(HealthCheckMessage message) {
        return message.getStatus() != null && message.getServiceName() != null;
    }
    
    /**
     * 구독 통계 조회
     */
    public SubscriptionStats getSubscriptionStats() {
        return SubscriptionStats.builder()
                .totalReceived(totalReceived.get())
                .totalProcessed(totalProcessed.get())
                .totalFailed(totalFailed.get())
                .totalIgnored(totalIgnored.get())
                .successRate(calculateSuccessRate())
                .build();
    }
    
    /**
     * 통계 초기화
     */
    public void resetStats() {
        totalReceived.set(0);
        totalProcessed.set(0);
        totalFailed.set(0);
        totalIgnored.set(0);
        log.info("구독 통계가 초기화되었습니다");
    }
    
    /**
     * 성공률 계산
     */
    private double calculateSuccessRate() {
        long total = totalReceived.get();
        if (total == 0) return 0.0;
        return ((double) totalProcessed.get() / total) * 100.0;
    }
    
    /**
     * 구독 통계 DTO
     */
    @lombok.Builder
    @lombok.Data
    public static class SubscriptionStats {
        private final long totalReceived;
        private final long totalProcessed;
        private final long totalFailed;
        private final long totalIgnored;
        private final double successRate;
        
        public long getTotalAttempted() {
            return totalReceived;
        }
        
        public double getFailureRate() {
            return totalReceived > 0 ? ((double) totalFailed / totalReceived) * 100.0 : 0.0;
        }
        
        public double getIgnoreRate() {
            return totalReceived > 0 ? ((double) totalIgnored / totalReceived) * 100.0 : 0.0;
        }
    }
}