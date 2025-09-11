package com.ocean.scdemo.redispubsub.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

/**
 * 사용자 이벤트 메시지
 * 
 * 사용자 행동 및 상태 변화 이벤트 처리
 * 로그인, 로그아웃, 구매, 상호작용 등
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserEventMessage extends BaseMessage {
    
    /**
     * 사용자 이벤트 기본 정보
     */
    private String eventType; // LOGIN, LOGOUT, REGISTER, PURCHASE, VIEW, CLICK, etc.
    private String userId;
    private String userName;
    private String userEmail;
    private String userRole;
    
    /**
     * 세션 정보
     */
    private String sessionId;
    private String sessionToken;
    private java.time.LocalDateTime sessionStartTime;
    private Long sessionDuration; // 세션 지속 시간 (ms)
    
    /**
     * 이벤트 내용
     */
    private String eventName;
    private String description;
    private String category; // USER_ACTION, USER_STATE, BUSINESS_EVENT
    private Map<String, Object> eventProperties;
    
    /**
     * 디바이스 및 위치 정보
     */
    private String deviceType; // WEB, MOBILE, TABLET
    private String deviceId;
    private String browser;
    private String operatingSystem;
    private String userAgent;
    private String ipAddress;
    private LocationData location;
    
    /**
     * 비즈니스 컨텍스트
     */
    private String pageUrl; // 웹 이벤트인 경우
    private String screenName; // 모바일 앱인 경우
    private String referrer;
    private String campaign; // 마케팅 캐페인
    private String source; // 유입 경로
    
    /**
     * 비즈니스 엔티티 정보
     */
    private String entityType; // PRODUCT, ORDER, CART, WISHLIST, etc.
    private String entityId; // 관련 엔티티 ID
    private Map<String, Object> entityData;
    
    /**
     * 이노멤 및 멤트릭스
     */
    private Double value; // 이벤트 값 (매출, 점수 등)
    private String currency; // 통화 단위
    private Integer quantity; // 수량
    private String status; // SUCCESS, FAILED, PENDING
    
    /**
     * A/B 테스트 및 개인화
     */
    private String experimentId; // A/B 테스트 ID
    private String variant; // 테스트 변형
    private Map<String, String> userSegments; // 사용자 세그먼트
    
    /**
     * 커스텀 속성
     */
    private Map<String, Object> customAttributes;
    private String[] tags; // 태그 배열
    
    /**
     * 사용자 이벤트 생성자 편의 메서드
     */
    public static UserEventMessage createLoginEvent(String userId, String userName, 
                                                   String sessionId, String deviceType) {
        return UserEventMessage.builder()
                .messageType("USER_EVENT")
                .eventType("LOGIN")
                .userId(userId)
                .userName(userName)
                .sessionId(sessionId)
                .deviceType(deviceType)
                .eventName("사용자 로그인")
                .description(userName + " 로그인")
                .category("USER_STATE")
                .status("SUCCESS")
                .sessionStartTime(java.time.LocalDateTime.now())
                .build();
    }
    
    public static UserEventMessage createPurchaseEvent(String userId, String productId, 
                                                       Double amount, String currency) {
        return UserEventMessage.builder()
                .messageType("USER_EVENT")
                .eventType("PURCHASE")
                .userId(userId)
                .entityType("PRODUCT")
                .entityId(productId)
                .eventName("상품 구매")
                .description("사용자가 상품을 구매했습니다")
                .category("BUSINESS_EVENT")
                .value(amount)
                .currency(currency)
                .quantity(1)
                .status("SUCCESS")
                .build();
    }
    
    public static UserEventMessage createPageViewEvent(String userId, String pageUrl, 
                                                       String referrer) {
        return UserEventMessage.builder()
                .messageType("USER_EVENT")
                .eventType("PAGE_VIEW")
                .userId(userId)
                .pageUrl(pageUrl)
                .referrer(referrer)
                .eventName("페이지 조회")
                .description("사용자가 페이지를 조회했습니다")
                .category("USER_ACTION")
                .status("SUCCESS")
                .build();
    }
    
    /**
     * 비즈니스 이벤트인지 확인
     */
    public boolean isBusinessEvent() {
        return "BUSINESS_EVENT".equals(category);
    }
    
    /**
     * 성공한 이벤트인지 확인
     */
    public boolean isSuccessEvent() {
        return "SUCCESS".equals(status);
    }
    
    /**
     * 세션 종료 처리
     */
    public void endSession() {
        if (sessionStartTime != null) {
            sessionDuration = java.time.Duration.between(
                sessionStartTime, 
                java.time.LocalDateTime.now()
            ).toMillis();
        }
    }
    
    /**
     * 커스텀 속성 추가
     */
    public void addCustomAttribute(String key, Object value) {
        if (customAttributes == null) {
            customAttributes = new java.util.HashMap<>();
        }
        customAttributes.put(key, value);
    }
    
    /**
     * 이벤트 속성 추가
     */
    public void addEventProperty(String key, Object value) {
        if (eventProperties == null) {
            eventProperties = new java.util.HashMap<>();
        }
        eventProperties.put(key, value);
    }
    
    /**
     * 엔티티 데이터 추가
     */
    public void addEntityData(String key, Object value) {
        if (entityData == null) {
            entityData = new java.util.HashMap<>();
        }
        entityData.put(key, value);
    }
    
    /**
     * A/B 테스트 설정
     */
    public void setExperiment(String experimentId, String variant) {
        this.experimentId = experimentId;
        this.variant = variant;
        addEventProperty("experiment", experimentId);
        addEventProperty("variant", variant);
    }
    
    @Override
    public BaseMessage copy() {
        return UserEventMessage.builder()
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
                .userId(this.userId)
                .userName(this.userName)
                .userEmail(this.userEmail)
                .userRole(this.userRole)
                .sessionId(this.sessionId)
                .sessionToken(this.sessionToken)
                .sessionStartTime(this.sessionStartTime)
                .sessionDuration(this.sessionDuration)
                .eventName(this.eventName)
                .description(this.description)
                .category(this.category)
                .eventProperties(this.eventProperties != null ? new java.util.HashMap<>(this.eventProperties) : null)
                .deviceType(this.deviceType)
                .deviceId(this.deviceId)
                .browser(this.browser)
                .operatingSystem(this.operatingSystem)
                .userAgent(this.userAgent)
                .ipAddress(this.ipAddress)
                .location(this.location)
                .pageUrl(this.pageUrl)
                .screenName(this.screenName)
                .referrer(this.referrer)
                .campaign(this.campaign)
                .source(this.source)
                .entityType(this.entityType)
                .entityId(this.entityId)
                .entityData(this.entityData != null ? new java.util.HashMap<>(this.entityData) : null)
                .value(this.value)
                .currency(this.currency)
                .quantity(this.quantity)
                .status(this.status)
                .experimentId(this.experimentId)
                .variant(this.variant)
                .userSegments(this.userSegments != null ? new java.util.HashMap<>(this.userSegments) : null)
                .customAttributes(this.customAttributes != null ? new java.util.HashMap<>(this.customAttributes) : null)
                .tags(this.tags != null ? this.tags.clone() : null)
                .build();
    }
    
    /**
     * 사용자 위치 데이터
     */
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationData {
        private Double latitude;
        private Double longitude;
        private String country;
        private String region;
        private String city;
        private String zipCode;
        private String timezone;
    }
}