package com.ocean.scdemo.redispubsub.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;

/**
 * 알림 메시지 모델
 * 
 * 시스템 알림, 사용자 알림, 푸시 알림 등을 지원
 * 다양한 알림 타입과 전달 방식을 처리
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NotificationMessage extends BaseMessage {
    
    /**
     * 알림 기본 정보
     */
    private String notificationType; // PUSH, EMAIL, SMS, IN_APP, SYSTEM
    private String category; // INFO, WARNING, ERROR, SUCCESS, MARKETING
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    
    /**
     * 수신자 정보
     */
    private String recipientId;
    private List<String> recipientIds; // 다중 수신자
    private String recipientGroup; // USER_GROUP, ADMIN_GROUP, ALL_USERS
    
    /**
     * 알림 내용
     */
    private String title;
    private String message;
    private String shortMessage; // 요약 메시지
    private String htmlContent; // HTML 내용
    
    /**
     * 대화형 액션 버튼
     */
    private List<ActionButton> actionButtons;
    
    /**
     * 링크 및 리다이렉션
     */
    private String clickUrl;
    private String deepLink;
    private String iconUrl;
    private String imageUrl;
    
    /**
     * 푸시 알림 설정
     */
    private PushConfig pushConfig;
    
    /**
     * 이메일 알림 설정
     */
    private EmailConfig emailConfig;
    
    /**
     * 상태 및 추적 정보
     */
    private String status; // PENDING, SENT, DELIVERED, READ, FAILED, EXPIRED
    private java.time.LocalDateTime scheduledAt; // 예약 전송 시간
    private java.time.LocalDateTime deliveredAt;
    private java.time.LocalDateTime readAt;
    private Integer readCount;
    private Integer clickCount;
    
    /**
     * 비즈니스 로직 정보
     */
    private String eventType; // USER_REGISTRATION, ORDER_CONFIRMED, PAYMENT_FAILED, etc.
    private String entityId; // 관련 엔티티 ID
    private String entityType; // USER, ORDER, PAYMENT, etc.
    private Map<String, Object> eventData;
    
    /**
     * 알림 생성자 편의 메서드
     */
    public static NotificationMessage createInfoNotification(String recipientId, 
                                                            String title, String message) {
        return NotificationMessage.builder()
                .messageType("NOTIFICATION")
                .notificationType("IN_APP")
                .category("INFO")
                .severity("LOW")
                .recipientId(recipientId)
                .title(title)
                .message(message)
                .status("PENDING")
                .build();
    }
    
    public static NotificationMessage createPushNotification(String recipientId,
                                                            String title, String message,
                                                            String clickUrl) {
        return NotificationMessage.builder()
                .messageType("NOTIFICATION")
                .notificationType("PUSH")
                .category("INFO")
                .severity("MEDIUM")
                .recipientId(recipientId)
                .title(title)
                .message(message)
                .clickUrl(clickUrl)
                .status("PENDING")
                .pushConfig(PushConfig.builder()
                        .sound("default")
                        .badge(1)
                        .priority("normal")
                        .build())
                .build();
    }
    
    public static NotificationMessage createSystemAlert(String title, String message, 
                                                       String severity) {
        return NotificationMessage.builder()
                .messageType("NOTIFICATION")
                .notificationType("SYSTEM")
                .category("WARNING")
                .severity(severity)
                .recipientGroup("ADMIN_GROUP")
                .title(title)
                .message(message)
                .status("PENDING")
                .build();
    }
    
    /**
     * 알림이 읽혔는지 확인
     */
    public boolean isRead() {
        return readAt != null;
    }
    
    /**
     * 알림이 만료되었는지 확인
     */
    public boolean isScheduled() {
        return scheduledAt != null && scheduledAt.isAfter(java.time.LocalDateTime.now());
    }
    
    /**
     * 알림 읽음 처리
     */
    public void markAsRead() {
        this.readAt = java.time.LocalDateTime.now();
        this.readCount = (this.readCount == null ? 0 : this.readCount) + 1;
        this.status = "READ".toUpperCase();
    }
    
    /**
     * 알림 클릭 처리
     */
    public void markAsClicked() {
        this.clickCount = (this.clickCount == null ? 0 : this.clickCount) + 1;
        if (!isRead()) {
            markAsRead();
        }
    }
    
    /**
     * 액션 버튼 추가
     */
    public void addActionButton(String title, String action, String url) {
        if (actionButtons == null) {
            actionButtons = new java.util.ArrayList<>();
        }
        actionButtons.add(ActionButton.builder()
                .title(title)
                .action(action)
                .url(url)
                .build());
    }
    
    @Override
    public BaseMessage copy() {
        return NotificationMessage.builder()
                .messageId(this.getMessageId())
                .messageType(this.getMessageType())
                .senderId(this.getSenderId())
                .senderName(this.getSenderName())
                .channel(this.getChannel())
                .topic(this.getTopic())
                .timestamp(this.getTimestamp())
                .priority(this.getPriority())
                .ttl(this.getTtl())
                .metadata(this.getMetadata() != null ? new java.util.HashMap<>(this.getMetadata()) : null)
                .notificationType(this.notificationType)
                .category(this.category)
                .severity(this.severity)
                .recipientId(this.recipientId)
                .recipientIds(this.recipientIds != null ? new java.util.ArrayList<>(this.recipientIds) : null)
                .recipientGroup(this.recipientGroup)
                .title(this.title)
                .message(this.message)
                .shortMessage(this.shortMessage)
                .htmlContent(this.htmlContent)
                .actionButtons(this.actionButtons != null ? new java.util.ArrayList<>(this.actionButtons) : null)
                .clickUrl(this.clickUrl)
                .deepLink(this.deepLink)
                .iconUrl(this.iconUrl)
                .imageUrl(this.imageUrl)
                .pushConfig(this.pushConfig)
                .emailConfig(this.emailConfig)
                .status(this.status)
                .scheduledAt(this.scheduledAt)
                .deliveredAt(this.deliveredAt)
                .readAt(this.readAt)
                .readCount(this.readCount)
                .clickCount(this.clickCount)
                .eventType(this.eventType)
                .entityId(this.entityId)
                .entityType(this.entityType)
                .eventData(this.eventData != null ? new java.util.HashMap<>(this.eventData) : null)
                .build();
    }
    
    /**
     * 액션 버튼 정보
     */
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionButton {
        private String title;
        private String action; // OPEN_URL, DEEP_LINK, CUSTOM_ACTION
        private String url;
        private Map<String, Object> parameters;
        private String iconUrl;
        private String color;
    }
    
    /**
     * 푸시 알림 설정
     */
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PushConfig {
        private String sound; // default, custom sound file
        private Integer badge;
        private String priority; // min, low, default, high, max
        private String collapseKey; // Android 전용
        private Long timeToLive; // TTL in seconds
        private Map<String, String> customData;
        private Boolean silentPush; // 무음 푸시 여부
    }
    
    /**
     * 이메일 알림 설정
     */
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailConfig {
        private String templateId;
        private String subject;
        private String fromEmail;
        private String fromName;
        private String replyTo;
        private List<String> ccEmails;
        private List<String> bccEmails;
        private Map<String, Object> templateVariables;
        private List<EmailAttachment> attachments;
    }
    
    /**
     * 이메일 첨부파일
     */
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailAttachment {
        private String fileName;
        private String fileUrl;
        private String contentType;
        private byte[] content;
    }
}