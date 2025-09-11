package com.ocean.scdemo.redispubsub.message;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Redis Pub/Sub 메시지의 기본 인터페이스
 * 
 * 모든 메시지 타입의 공통 속성과 메서드를 정의
 * Jackson의 다형성 직렬화를 위한 타입 정보 포함
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ChatMessage.class, name = "ChatMessage"),
    @JsonSubTypes.Type(value = NotificationMessage.class, name = "NotificationMessage"),
    @JsonSubTypes.Type(value = SystemEventMessage.class, name = "SystemEventMessage"),
    @JsonSubTypes.Type(value = UserEventMessage.class, name = "UserEventMessage"),
    @JsonSubTypes.Type(value = MetricsMessage.class, name = "MetricsMessage"),
    @JsonSubTypes.Type(value = HealthCheckMessage.class, name = "HealthCheckMessage")
})
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseMessage {
    
    /**
     * 메시지 고유 ID
     */
    private String messageId = UUID.randomUUID().toString();
    
    /**
     * 메시지 타입 (채팅, 알림, 이벤트 등)
     */
    private String messageType;
    
    /**
     * 발신자 정보
     */
    private String senderId;
    private String senderName;
    
    /**
     * 채널/토픽 정보
     */
    private String channel;
    private String topic;
    
    /**
     * 타임스탬프
     */
    private LocalDateTime timestamp = LocalDateTime.now();
    
    /**
     * 메시지 우선순위 (1: 높음, 2: 보통, 3: 낮음)
     */
    private Integer priority = 2;
    
    /**
     * TTL (Time To Live) - 메시지 유효 시간 (초)
     */
    private Long ttl;
    
    /**
     * 추가 메타데이터
     */
    private Map<String, Object> metadata;
    
    /**
     * 메시지 크기 (바이트)
     */
    private Integer messageSize;
    
    /**
     * 재시도 카운트
     */
    private Integer retryCount = 0;
    
    /**
     * 메시지 체크섬 (무결성 검증용)
     */
    private String checksum;
    
    /**
     * 메시지 그룹 ID (배치 처리용)
     */
    private String groupId;
    
    /**
     * 메시지 시퀀스 번호
     */
    private Long sequenceNumber;
    
    /**
     * 메시지 압축 여부
     */
    private Boolean compressed = false;
    
    /**
     * 메시지 암호화 여부
     */
    private Boolean encrypted = false;
    
    /**
     * 메시지 만료 시간 체크
     */
    public boolean isExpired() {
        if (ttl == null) {
            return false;
        }
        return timestamp.plusSeconds(ttl).isBefore(LocalDateTime.now());
    }
    
    /**
     * 메시지 우선순위 체크
     */
    public boolean isHighPriority() {
        return priority != null && priority == 1;
    }
    
    /**
     * 메타데이터 추가
     */
    public void addMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new java.util.HashMap<>();
        }
        metadata.put(key, value);
    }
    
    /**
     * 메타데이터 조회
     */
    public Object getMetadata(String key) {
        return metadata != null ? metadata.get(key) : null;
    }
    
    /**
     * 메시지 검증
     */
    public boolean isValid() {
        return messageId != null && 
               messageType != null && 
               !isExpired() && 
               retryCount < 5; // 최대 5회 재시도
    }
    
    /**
     * 재시도 카운트 증가
     */
    public void incrementRetryCount() {
        this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
    }
    
    /**
     * 메시지 요약 정보
     */
    public String getSummary() {
        return String.format("%s[id=%s, type=%s, sender=%s, channel=%s, timestamp=%s]", 
                           getClass().getSimpleName(), 
                           messageId, 
                           messageType, 
                           senderId, 
                           channel, 
                           timestamp);
    }
    
    /**
     * 메시지 복사 (깊은 복사)
     */
    public abstract BaseMessage copy();
}