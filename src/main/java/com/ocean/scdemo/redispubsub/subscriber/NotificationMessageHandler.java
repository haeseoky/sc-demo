package com.ocean.scdemo.redispubsub.subscriber;
import com.ocean.scdemo.redispubsub.message.NotificationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;
/**
 * 알림 메시지 처리 핸들러
 * 
 * 기능:
 * - 푸시 알림 전송
 * - 이메일 알림 전송  
 * - SMS 알림 전송
 * - 인앱 알림 처리
 * - 시스템 알림 처리
 * - 알림 우선순위별 라우팅
 * - 알림 전송 이력 추적
 */
@Slf4j
@Component
public class NotificationMessageHandler {
    // 처리 통계
    private final AtomicLong totalProcessed = new AtomicLong(0);
    private final AtomicLong pushNotifications = new AtomicLong(0);
    private final AtomicLong emailNotifications = new AtomicLong(0);
    private final AtomicLong smsNotifications = new AtomicLong(0);
    private final AtomicLong inAppNotifications = new AtomicLong(0);
    private final AtomicLong systemNotifications = new AtomicLong(0);
    private final AtomicLong highPriorityNotifications = new AtomicLong(0);
    private final AtomicLong scheduledNotifications = new AtomicLong(0);
    
    public boolean handleMessage(NotificationMessage message) {
        try {
            totalProcessed.incrementAndGet();
            
            // 예약된 알림인지 확인
            if (message.isScheduled()) {
                return handleScheduledNotification(message);
            }
            
            // 우선순위 체크
            if (isHighPriority(message)) {
                highPriorityNotifications.incrementAndGet();
                log.info("🚨 높은 우선순위 알림: ID={}, 제목={}, 수신자={}", 
                        message.getMessageId(), message.getTitle(), message.getRecipientId());
            }
            
            // 알림 타입별 처리
            boolean processed = switch (message.getNotificationType()) {
                case "PUSH" -> handlePushNotification(message);
                case "EMAIL" -> handleEmailNotification(message);
                case "SMS" -> handleSmsNotification(message);
                case "IN_APP" -> handleInAppNotification(message);
                case "SYSTEM" -> handleSystemNotification(message);
                default -> handleGenericNotification(message);
            };
            
            if (processed) {
                // 전송 완료 처리
                message.setDeliveredAt(LocalDateTime.now());
                message.setStatus("DELIVERED");
                
                log.debug("알림 처리 완료: ID={}, 타입={}, 수신자={}", 
                         message.getMessageId(), message.getNotificationType(), message.getRecipientId());
            }
            
            return processed;
            
        } catch (Exception e) {
            log.error("알림 메시지 처리 실패: ID={}", message.getMessageId(), e);
            return false;
        }
    }
    
    /**
     * 푸시 알림 처리
     */
    private boolean handlePushNotification(NotificationMessage message) {
        pushNotifications.incrementAndGet();
        
        log.info("📱 푸시 알림: 수신자={}, 제목='{}', 메시지='{}'", 
                message.getRecipientId(), message.getTitle(), message.getMessage());
        
        // 푸시 알림 설정 확인
        NotificationMessage.PushConfig pushConfig = message.getPushConfig();
        if (pushConfig != null) {
            log.debug("푸시 설정: 소리={}, 뱃지={}, 우선순위={}", 
                     pushConfig.getSound(), pushConfig.getBadge(), pushConfig.getPriority());
            
            // 무음 푸시 처리
            if (pushConfig.getSilentPush() != null && pushConfig.getSilentPush()) {
                log.debug("무음 푸시 알림 전송: ID={}", message.getMessageId());
                return sendSilentPush(message);
            }
        }
        
        // 액션 버튼 처리
        if (message.getActionButtons() != null && !message.getActionButtons().isEmpty()) {
            log.debug("액션 버튼 포함 푸시: 버튼 수={}", message.getActionButtons().size());
            return sendInteractivePush(message);
        }
        
        // 일반 푸시 전송
        return sendStandardPush(message);
    }
    
    /**
     * 이메일 알림 처리
     */
    private boolean handleEmailNotification(NotificationMessage message) {
        emailNotifications.incrementAndGet();
        
        log.info("📧 이메일 알림: 수신자={}, 제목='{}'", 
                message.getRecipientId(), message.getTitle());
        
        // 이메일 설정 확인
        NotificationMessage.EmailConfig emailConfig = message.getEmailConfig();
        if (emailConfig != null) {
            log.debug("이메일 설정: 템플릿={}, 발신자={}, 참조={}", 
                     emailConfig.getTemplateId(), emailConfig.getFromEmail(), 
                     emailConfig.getCcEmails() != null ? emailConfig.getCcEmails().size() : 0);
            
            // 템플릿 이메일 전송
            if (emailConfig.getTemplateId() != null) {
                return sendTemplateEmail(message, emailConfig);
            }
            
            // 첨부파일이 있는 이메일
            if (emailConfig.getAttachments() != null && !emailConfig.getAttachments().isEmpty()) {
                return sendEmailWithAttachments(message, emailConfig);
            }
        }
        
        // 일반 이메일 전송
        return sendStandardEmail(message);
    }
    
    /**
     * SMS 알림 처리
     */
    private boolean handleSmsNotification(NotificationMessage message) {
        smsNotifications.incrementAndGet();
        
        log.info("📱 SMS 알림: 수신자={}, 메시지='{}'", 
                message.getRecipientId(), message.getShortMessage() != null ? 
                message.getShortMessage() : message.getMessage());
        
        // SMS는 길이 제한이 있으므로 짧은 메시지 사용
        String smsContent = message.getShortMessage() != null ? 
                           message.getShortMessage() : 
                           truncateForSms(message.getMessage());
        
        if (smsContent.length() > 160) {
            log.warn("SMS 메시지가 너무 길어서 잘림: 원본길이={}, 잘린길이={}", 
                    message.getMessage().length(), smsContent.length());
        }
        
        return sendSms(message.getRecipientId(), smsContent);
    }
    
    /**
     * 인앱 알림 처리
     */
    private boolean handleInAppNotification(NotificationMessage message) {
        inAppNotifications.incrementAndGet();
        
        log.info("📲 인앱 알림: 수신자={}, 제목='{}'", 
                message.getRecipientId(), message.getTitle());
        
        // 인앱 알림은 즉시 표시
        return sendInAppNotification(message);
    }
    
    /**
     * 시스템 알림 처리
     */
    private boolean handleSystemNotification(NotificationMessage message) {
        systemNotifications.incrementAndGet();
        
        log.info("⚙️ 시스템 알림: 그룹={}, 제목='{}', 심각도={}", 
                message.getRecipientGroup(), message.getTitle(), message.getSeverity());
        
        // 심각도에 따른 처리
        return switch (message.getSeverity()) {
            case "CRITICAL" -> handleCriticalSystemNotification(message);
            case "HIGH" -> handleHighSystemNotification(message);
            case "MEDIUM" -> handleMediumSystemNotification(message);
            default -> handleLowSystemNotification(message);
        };
    }
    
    /**
     * 일반 알림 처리
     */
    private boolean handleGenericNotification(NotificationMessage message) {
        log.info("🔔 일반 알림: 타입={}, 수신자={}, 제목='{}'", 
                message.getNotificationType(), message.getRecipientId(), message.getTitle());
        
        return true; // 기본 처리 성공
    }
    
    /**
     * 예약된 알림 처리
     */
    private boolean handleScheduledNotification(NotificationMessage message) {
        scheduledNotifications.incrementAndGet();
        
        log.info("⏰ 예약 알림: ID={}, 예약시간={}, 현재시간={}", 
                message.getMessageId(), message.getScheduledAt(), LocalDateTime.now());
        
        // 예약 시간이 아직 되지 않았으면 대기
        if (message.getScheduledAt().isAfter(LocalDateTime.now())) {
            log.debug("예약 시간 미도래, 처리 지연: ID={}", message.getMessageId());
            // 실제로는 스케줄러에 다시 등록하거나 지연 처리
            return false;
        }
        
        // 예약 시간이 된 경우 일반 처리 진행
        message.setScheduledAt(null); // 예약 플래그 제거
        return handleMessage(message); // 재귀 호출로 일반 처리
    }
    
    // === 실제 전송 메서드들 (모의 구현) ===
    
    private boolean sendStandardPush(NotificationMessage message) {
        // 실제 푸시 서비스 (FCM, APNS) 연동
        log.debug("표준 푸시 전송: {}", message.getMessageId());
        return true;
    }
    
    private boolean sendSilentPush(NotificationMessage message) {
        // 백그라운드 데이터 동기화용 무음 푸시
        log.debug("무음 푸시 전송: {}", message.getMessageId());
        return true;
    }
    
    private boolean sendInteractivePush(NotificationMessage message) {
        // 액션 버튼이 포함된 대화형 푸시
        log.debug("대화형 푸시 전송: {}", message.getMessageId());
        return true;
    }
    
    private boolean sendStandardEmail(NotificationMessage message) {
        // 일반 이메일 전송
        log.debug("표준 이메일 전송: {}", message.getMessageId());
        return true;
    }
    
    private boolean sendTemplateEmail(NotificationMessage message, 
                                     NotificationMessage.EmailConfig config) {
        // 템플릿 기반 이메일 전송
        log.debug("템플릿 이메일 전송: 템플릿={}", config.getTemplateId());
        return true;
    }
    
    private boolean sendEmailWithAttachments(NotificationMessage message, 
                                           NotificationMessage.EmailConfig config) {
        // 첨부파일 포함 이메일 전송
        log.debug("첨부파일 이메일 전송: 첨부 수={}", config.getAttachments().size());
        return true;
    }
    
    private boolean sendSms(String recipient, String content) {
        // SMS 전송 서비스 연동
        log.debug("SMS 전송: 수신자={}, 길이={}", recipient, content.length());
        return true;
    }
    
    private boolean sendInAppNotification(NotificationMessage message) {
        // WebSocket이나 SSE를 통한 실시간 인앱 알림
        log.debug("인앱 알림 전송: {}", message.getMessageId());
        return true;
    }
    
    // === 시스템 알림 심각도별 처리 ===
    
    private boolean handleCriticalSystemNotification(NotificationMessage message) {
        log.error("🚨 심각한 시스템 알림: {}", message.getMessage());
        // 즉시 관리자에게 전화, 페이저 등 모든 채널로 알림
        // escalateToManagement(message);
        return true;
    }
    
    private boolean handleHighSystemNotification(NotificationMessage message) {
        log.warn("⚠️ 높은 우선순위 시스템 알림: {}", message.getMessage());
        // 관리자 그룹에 푸시 + 이메일 + 슬랙 알림
        // sendToAdminGroup(message);
        return true;
    }
    
    private boolean handleMediumSystemNotification(NotificationMessage message) {
        log.info("ℹ️ 중간 우선순위 시스템 알림: {}", message.getMessage());
        // 관리자 그룹에 이메일 + 슬랙 알림
        return true;
    }
    
    private boolean handleLowSystemNotification(NotificationMessage message) {
        log.debug("📝 낮은 우선순위 시스템 알림: {}", message.getMessage());
        // 로그만 남기거나 일일 요약에 포함
        return true;
    }
    
    // === 유틸리티 메서드들 ===
    
    /**
     * 높은 우선순위 알림인지 확인
     */
    private boolean isHighPriority(NotificationMessage message) {
        return message.isHighPriority() || 
               "CRITICAL".equals(message.getSeverity()) || 
               "HIGH".equals(message.getSeverity());
    }
    
    /**
     * SMS용 메시지 길이 제한
     */
    private String truncateForSms(String message) {
        if (message == null) return "";
        if (message.length() <= 160) return message;
        return message.substring(0, 157) + "...";
    }
    
    /**
     * 핸들러 통계 조회
     */
    public NotificationHandlerStats getStats() {
        return NotificationHandlerStats.builder()
                .totalProcessed(totalProcessed.get())
                .pushNotifications(pushNotifications.get())
                .emailNotifications(emailNotifications.get())
                .smsNotifications(smsNotifications.get())
                .inAppNotifications(inAppNotifications.get())
                .systemNotifications(systemNotifications.get())
                .highPriorityNotifications(highPriorityNotifications.get())
                .scheduledNotifications(scheduledNotifications.get())
                .build();
    }
    
    /**
     * 알림 핸들러 통계 DTO
     */
    @lombok.Builder
    @lombok.Data
    public static class NotificationHandlerStats {
        private final long totalProcessed;
        private final long pushNotifications;
        private final long emailNotifications;
        private final long smsNotifications;
        private final long inAppNotifications;
        private final long systemNotifications;
        private final long highPriorityNotifications;
        private final long scheduledNotifications;
        
        public double getHighPriorityRate() {
            return totalProcessed > 0 ? 
                   ((double) highPriorityNotifications / totalProcessed) * 100.0 : 0.0;
        }
        
        public double getSuccessRate() {
            return totalProcessed > 0 ? 100.0 : 0.0; // 간단한 구현
        }
    }
}