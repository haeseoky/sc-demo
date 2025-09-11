package com.ocean.scdemo.redispubsub.subscriber;

import com.ocean.scdemo.redispubsub.message.UserEventMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 사용자 이벤트 메시지 처리 핸들러
 * 
 * 기능:
 * - 사용자 로그인/로그아웃 처리
 * - 사용자 행동 분석 및 추적
 * - 비즈니스 이벤트 처리 (구매, 클릭 등)
 * - 개인화 데이터 수집
 * - 사용자 세그멘테이션
 * - A/B 테스트 결과 수집
 */
@Slf4j
@Component
public class UserEventMessageHandler {

    // 처리 통계
    private final AtomicLong totalProcessed = new AtomicLong(0);
    private final AtomicLong loginEvents = new AtomicLong(0);
    private final AtomicLong logoutEvents = new AtomicLong(0);
    private final AtomicLong purchaseEvents = new AtomicLong(0);
    private final AtomicLong pageViewEvents = new AtomicLong(0);
    private final AtomicLong clickEvents = new AtomicLong(0);
    private final AtomicLong businessEvents = new AtomicLong(0);
    
    // 활성 사용자 세션 추적
    private final Map<String, UserSession> activeSessions = new ConcurrentHashMap<>();
    
    // 사용자별 이벤트 카운트 (일일 기준)
    private final Map<String, UserDailyStats> dailyUserStats = new ConcurrentHashMap<>();
    
    public boolean handleMessage(UserEventMessage message) {
        try {
            totalProcessed.incrementAndGet();
            
            // 이벤트 타입별 처리
            boolean processed = switch (message.getEventType()) {
                case "LOGIN" -> handleLoginEvent(message);
                case "LOGOUT" -> handleLogoutEvent(message);
                case "PURCHASE" -> handlePurchaseEvent(message);
                case "PAGE_VIEW" -> handlePageViewEvent(message);
                case "CLICK" -> handleClickEvent(message);
                case "REGISTER" -> handleRegisterEvent(message);
                case "PROFILE_UPDATE" -> handleProfileUpdateEvent(message);
                default -> handleGenericUserEvent(message);
            };
            
            if (processed) {
                // 사용자 활동 추적
                updateUserActivity(message);
                
                // 개인화 데이터 업데이트
                updatePersonalizationData(message);
                
                // A/B 테스트 결과 수집
                collectAbTestData(message);
                
                log.debug("사용자 이벤트 처리 완료: 사용자={}, 이벤트={}, 카테고리={}", 
                         message.getUserId(), message.getEventType(), message.getCategory());
            }
            
            return processed;
            
        } catch (Exception e) {
            log.error("사용자 이벤트 처리 실패: ID={}", message.getMessageId(), e);
            return false;
        }
    }
    
    /**
     * 로그인 이벤트 처리
     */
    private boolean handleLoginEvent(UserEventMessage message) {
        loginEvents.incrementAndGet();
        
        log.info("👤 사용자 로그인: 사용자={}, 기기={}, 위치={}", 
                message.getUserId(), message.getDeviceType(), 
                message.getLocation() != null ? message.getLocation().getCity() : "unknown");
        
        // 세션 시작
        UserSession session = UserSession.builder()
                .userId(message.getUserId())
                .sessionId(message.getSessionId())
                .loginTime(LocalDateTime.now())
                .deviceType(message.getDeviceType())
                .ipAddress(message.getIpAddress())
                .userAgent(message.getUserAgent())
                .build();
        
        activeSessions.put(message.getSessionId(), session);
        
        // 로그인 기록 및 분석
        recordLoginAttempt(message);
        
        // 보안 체크 (비정상적인 로그인 패턴 감지)
        checkSuspiciousLogin(message);
        
        return true;
    }
    
    /**
     * 로그아웃 이벤트 처리
     */
    private boolean handleLogoutEvent(UserEventMessage message) {
        logoutEvents.incrementAndGet();
        
        log.info("👋 사용자 로그아웃: 사용자={}, 세션시간={}", 
                message.getUserId(), 
                message.getSessionDuration() != null ? message.getSessionDuration() / 1000 + "초" : "unknown");
        
        // 세션 종료
        UserSession session = activeSessions.remove(message.getSessionId());
        if (session != null) {
            session.setLogoutTime(LocalDateTime.now());
            // 세션 통계 업데이트
            updateSessionStats(session, message);
        }
        
        return true;
    }
    
    /**
     * 구매 이벤트 처리
     */
    private boolean handlePurchaseEvent(UserEventMessage message) {
        purchaseEvents.incrementAndGet();
        businessEvents.incrementAndGet();
        
        log.info("🛒 구매 이벤트: 사용자={}, 상품={}, 금액={} {}, 수량={}", 
                message.getUserId(), message.getEntityId(), 
                message.getValue(), message.getCurrency(), message.getQuantity());
        
        // 구매 통계 업데이트
        updatePurchaseStats(message);
        
        // 추천 시스템 데이터 업데이트
        updateRecommendationData(message);
        
        // 고객 등급 재계산
        recalculateCustomerTier(message.getUserId());
        
        return true;
    }
    
    /**
     * 페이지 조회 이벤트 처리
     */
    private boolean handlePageViewEvent(UserEventMessage message) {
        pageViewEvents.incrementAndGet();
        
        log.debug("📄 페이지 조회: 사용자={}, 페이지={}, 참조={}", 
                 message.getUserId(), message.getPageUrl(), message.getReferrer());
        
        // 페이지뷰 통계 업데이트
        updatePageViewStats(message);
        
        // 사용자 관심사 분석
        analyzeUserInterests(message);
        
        return true;
    }
    
    /**
     * 클릭 이벤트 처리
     */
    private boolean handleClickEvent(UserEventMessage message) {
        clickEvents.incrementAndGet();
        
        log.debug("🖱️ 클릭 이벤트: 사용자={}, 페이지={}, 요소={}", 
                 message.getUserId(), message.getPageUrl(), 
                 message.getEventProperties() != null ? message.getEventProperties().get("element") : "unknown");
        
        // 클릭 패턴 분석
        analyzeClickPattern(message);
        
        // 전환율 추적
        trackConversion(message);
        
        return true;
    }
    
    /**
     * 회원가입 이벤트 처리
     */
    private boolean handleRegisterEvent(UserEventMessage message) {
        log.info("🎉 신규 회원가입: 사용자={}, 이메일={}, 소스={}", 
                message.getUserId(), message.getUserEmail(), message.getSource());
        
        // 신규 사용자 온보딩 시작
        triggerOnboardingFlow(message);
        
        // 가입 경로 분석
        analyzeRegistrationSource(message);
        
        return true;
    }
    
    /**
     * 프로필 업데이트 이벤트 처리
     */
    private boolean handleProfileUpdateEvent(UserEventMessage message) {
        log.info("✏️ 프로필 업데이트: 사용자={}", message.getUserId());
        
        // 프로필 변경 이력 추적
        trackProfileChanges(message);
        
        return true;
    }
    
    /**
     * 일반 사용자 이벤트 처리
     */
    private boolean handleGenericUserEvent(UserEventMessage message) {
        log.debug("📊 사용자 이벤트: 사용자={}, 이벤트={}, 카테고리={}", 
                 message.getUserId(), message.getEventType(), message.getCategory());
        
        return true;
    }
    
    // === 데이터 분석 및 업데이트 메서드들 ===
    
    private void updateUserActivity(UserEventMessage message) {
        String userId = message.getUserId();
        UserDailyStats stats = dailyUserStats.computeIfAbsent(userId, k -> new UserDailyStats());
        stats.incrementEventCount();
        stats.setLastActivityTime(LocalDateTime.now());
    }
    
    private void updatePersonalizationData(UserEventMessage message) {
        // 개인화 추천을 위한 데이터 업데이트
        if (message.getEventProperties() != null) {
            // 사용자 선호도, 관심사, 행동 패턴 분석
        }
    }
    
    private void collectAbTestData(UserEventMessage message) {
        if (message.getExperimentId() != null && message.getVariant() != null) {
            log.debug("A/B 테스트 데이터: 실험={}, 변형={}, 사용자={}", 
                     message.getExperimentId(), message.getVariant(), message.getUserId());
            
            // A/B 테스트 결과 수집 및 분석
        }
    }
    
    private void recordLoginAttempt(UserEventMessage message) {
        // 로그인 시도 기록 및 패턴 분석
    }
    
    private void checkSuspiciousLogin(UserEventMessage message) {
        // 비정상적인 로그인 패턴 감지 (예: 다른 국가에서의 로그인)
        if (message.getLocation() != null) {
            // 지역 기반 보안 체크
        }
    }
    
    private void updateSessionStats(UserSession session, UserEventMessage message) {
        // 세션 통계 업데이트 (세션 시간, 페이지뷰 수 등)
    }
    
    private void updatePurchaseStats(UserEventMessage message) {
        // 구매 통계 및 매출 데이터 업데이트
    }
    
    private void updateRecommendationData(UserEventMessage message) {
        // 추천 시스템을 위한 구매 데이터 업데이트
    }
    
    private void recalculateCustomerTier(String userId) {
        // 구매 이력 기반으로 고객 등급 재계산
    }
    
    private void updatePageViewStats(UserEventMessage message) {
        // 페이지별 조회수, 체류시간 등 통계 업데이트
    }
    
    private void analyzeUserInterests(UserEventMessage message) {
        // 페이지 내용 기반으로 사용자 관심사 분석
    }
    
    private void analyzeClickPattern(UserEventMessage message) {
        // 클릭 패턴 분석으로 UI/UX 개선점 도출
    }
    
    private void trackConversion(UserEventMessage message) {
        // 전환 퍼널 분석 및 전환율 추적
    }
    
    private void triggerOnboardingFlow(UserEventMessage message) {
        // 신규 사용자 온보딩 프로세스 시작
    }
    
    private void analyzeRegistrationSource(UserEventMessage message) {
        // 가입 경로별 분석 및 마케팅 효과 측정
    }
    
    private void trackProfileChanges(UserEventMessage message) {
        // 프로필 변경 이력 및 사용자 행동 분석
    }
    
    /**
     * 핸들러 통계 조회
     */
    public UserEventHandlerStats getStats() {
        return UserEventHandlerStats.builder()
                .totalProcessed(totalProcessed.get())
                .loginEvents(loginEvents.get())
                .logoutEvents(logoutEvents.get())
                .purchaseEvents(purchaseEvents.get())
                .pageViewEvents(pageViewEvents.get())
                .clickEvents(clickEvents.get())
                .businessEvents(businessEvents.get())
                .activeSessions(activeSessions.size())
                .activeUsers(dailyUserStats.size())
                .build();
    }
    
    // === DTO 클래스들 ===
    
    @lombok.Builder
    @lombok.Data
    public static class UserSession {
        private String userId;
        private String sessionId;
        private LocalDateTime loginTime;
        private LocalDateTime logoutTime;
        private String deviceType;
        private String ipAddress;
        private String userAgent;
        
        public long getSessionDurationMinutes() {
            if (logoutTime == null) return 0;
            return java.time.Duration.between(loginTime, logoutTime).toMinutes();
        }
    }
    
    @lombok.Data
    public static class UserDailyStats {
        private int eventCount = 0;
        private LocalDateTime lastActivityTime;
        private LocalDateTime firstActivityTime = LocalDateTime.now();
        
        public void incrementEventCount() {
            eventCount++;
        }
        
        public boolean isActiveToday() {
            return lastActivityTime != null && 
                   lastActivityTime.toLocalDate().equals(LocalDateTime.now().toLocalDate());
        }
    }
    
    @lombok.Builder
    @lombok.Data
    public static class UserEventHandlerStats {
        private final long totalProcessed;
        private final long loginEvents;
        private final long logoutEvents;
        private final long purchaseEvents;
        private final long pageViewEvents;
        private final long clickEvents;
        private final long businessEvents;
        private final int activeSessions;
        private final int activeUsers;
        
        public double getBusinessEventRate() {
            return totalProcessed > 0 ? 
                   ((double) businessEvents / totalProcessed) * 100.0 : 0.0;
        }
        
        public double getEngagementRate() {
            return totalProcessed > 0 ? 
                   ((double) (pageViewEvents + clickEvents) / totalProcessed) * 100.0 : 0.0;
        }
        
        public double getSuccessRate() {
            return totalProcessed > 0 ? 100.0 : 0.0; // 간단한 구현
        }
    }
}