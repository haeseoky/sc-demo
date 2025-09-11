package com.ocean.scdemo.redispubsub.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

/**
 * 시스템 이벤트 메시지
 * 
 * 시스템 레벨의 이벤트를 처리
 * 서비스 상태, 성능 지표, 에러 발생 등
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SystemEventMessage extends BaseMessage {
    
    /**
     * 이벤트 기본 정보
     */
    private String eventType; // SERVER_START, SERVER_STOP, DEPLOY, ERROR, ALERT
    private String eventSource; // SERVICE_NAME, MODULE, COMPONENT
    private String eventLevel; // DEBUG, INFO, WARN, ERROR, FATAL
    
    /**
     * 시스템 정보
     */
    private String serviceName;
    private String serviceVersion;
    private String instanceId;
    private String hostName;
    private String environment; // DEV, STAGING, PROD
    
    /**
     * 이벤트 내용
     */
    private String eventName;
    private String description;
    private String details;
    private Map<String, Object> eventData;
    
    /**
     * 오류 정보 (에러 이벤트인 경우)
     */
    private String errorCode;
    private String errorMessage;
    private String stackTrace;
    private String causedBy;
    
    /**
     * 성능 메트릭스
     */
    private Long responseTime; // 응답시간 (ms)
    private Double cpuUsage;
    private Double memoryUsage;
    private Double diskUsage;
    private Integer activeConnections;
    private Integer threadCount;
    
    /**
     * 시간 및 대상 정보
     */
    private java.time.LocalDateTime eventStartTime;
    private java.time.LocalDateTime eventEndTime;
    private Long duration; // 이벤트 지속 시간 (ms)
    private String affectedResource; // 영향을 받는 리소스
    
    /**
     * 상태 및 추적 정보
     */
    private String status; // STARTED, IN_PROGRESS, COMPLETED, FAILED
    private String correlationId; // 연관된 이벤트 추적용
    private String traceId; // 분산 추적 ID
    private String spanId; // Span ID
    
    /**
     * 알림 및 에스케이레이션 설정
     */
    private Boolean requiresAlert; // 알림 필요 여부
    private String alertLevel; // LOW, MEDIUM, HIGH, CRITICAL
    private Boolean requiresEscalation; // 에스케이레이션 필요 여부
    private String responsibleTeam; // 처리 담당 팀
    
    /**
     * 시스템 이벤트 생성자 편의 메서드
     */
    public static SystemEventMessage createServerStartEvent(String serviceName, String instanceId) {
        return SystemEventMessage.builder()
                .messageType("SYSTEM_EVENT")
                .eventType("SERVER_START")
                .eventSource(serviceName)
                .eventLevel("INFO")
                .serviceName(serviceName)
                .instanceId(instanceId)
                .eventName("서비스 시작")
                .description(serviceName + " 서비스가 시작되었습니다")
                .status("COMPLETED")
                .eventStartTime(java.time.LocalDateTime.now())
                .build();
    }
    
    public static SystemEventMessage createErrorEvent(String serviceName, String errorMessage, 
                                                     String stackTrace) {
        return SystemEventMessage.builder()
                .messageType("SYSTEM_EVENT")
                .eventType("ERROR")
                .eventSource(serviceName)
                .eventLevel("ERROR")
                .serviceName(serviceName)
                .eventName("시스템 오류")
                .description("시스템에서 오류가 발생했습니다")
                .errorMessage(errorMessage)
                .stackTrace(stackTrace)
                .status("FAILED")
                .requiresAlert(true)
                .alertLevel("HIGH")
                .eventStartTime(java.time.LocalDateTime.now())
                .build();
    }
    
    public static SystemEventMessage createPerformanceAlert(String serviceName, 
                                                           Long responseTime, 
                                                           Double cpuUsage) {
        return SystemEventMessage.builder()
                .messageType("SYSTEM_EVENT")
                .eventType("PERFORMANCE_ALERT")
                .eventSource(serviceName)
                .eventLevel("WARN")
                .serviceName(serviceName)
                .eventName("성능 경고")
                .description("시스템 성능이 임계치를 초과했습니다")
                .responseTime(responseTime)
                .cpuUsage(cpuUsage)
                .status("IN_PROGRESS")
                .requiresAlert(true)
                .alertLevel("MEDIUM")
                .eventStartTime(java.time.LocalDateTime.now())
                .build();
    }
    
    /**
     * 오류 이벤트인지 확인
     */
    public boolean isErrorEvent() {
        return "ERROR".equals(eventLevel) || "FATAL".equals(eventLevel);
    }
    
    /**
     * 알림이 필요한지 확인
     */
    public boolean needsAlert() {
        return requiresAlert != null && requiresAlert;
    }
    
    /**
     * 에스케이레이션이 필요한지 확인
     */
    public boolean needsEscalation() {
        return requiresEscalation != null && requiresEscalation;
    }
    
    /**
     * 이벤트 지속 시간 계산
     */
    public void completeEvent() {
        this.eventEndTime = java.time.LocalDateTime.now();
        this.status = "COMPLETED";
        
        if (eventStartTime != null) {
            this.duration = java.time.Duration.between(eventStartTime, eventEndTime).toMillis();
        }
    }
    
    /**
     * 이벤트 데이터 추가
     */
    public void addEventData(String key, Object value) {
        if (eventData == null) {
            eventData = new java.util.HashMap<>();
        }
        eventData.put(key, value);
    }
    
    /**
     * 상관 이벤트와 연결
     */
    public void linkToCorrelationId(String correlationId) {
        this.correlationId = correlationId;
        addEventData("linkedEvents", correlationId);
    }
    
    @Override
    public BaseMessage copy() {
        return SystemEventMessage.builder()
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
                .eventType(this.eventType)
                .eventSource(this.eventSource)
                .eventLevel(this.eventLevel)
                .serviceName(this.serviceName)
                .serviceVersion(this.serviceVersion)
                .instanceId(this.instanceId)
                .hostName(this.hostName)
                .environment(this.environment)
                .eventName(this.eventName)
                .description(this.description)
                .details(this.details)
                .eventData(this.eventData != null ? new java.util.HashMap<>(this.eventData) : null)
                .errorCode(this.errorCode)
                .errorMessage(this.errorMessage)
                .stackTrace(this.stackTrace)
                .causedBy(this.causedBy)
                .responseTime(this.responseTime)
                .cpuUsage(this.cpuUsage)
                .memoryUsage(this.memoryUsage)
                .diskUsage(this.diskUsage)
                .activeConnections(this.activeConnections)
                .threadCount(this.threadCount)
                .eventStartTime(this.eventStartTime)
                .eventEndTime(this.eventEndTime)
                .duration(this.duration)
                .affectedResource(this.affectedResource)
                .status(this.status)
                .correlationId(this.correlationId)
                .traceId(this.traceId)
                .spanId(this.spanId)
                .requiresAlert(this.requiresAlert)
                .alertLevel(this.alertLevel)
                .requiresEscalation(this.requiresEscalation)
                .responsibleTeam(this.responsibleTeam)
                .build();
    }
}