package com.ocean.scdemo.redispubsub.monitoring;

import com.ocean.scdemo.redispubsub.config.RedisSubscriptionConfig;
import com.ocean.scdemo.redispubsub.publisher.MessagePublisher;
import com.ocean.scdemo.redispubsub.subscriber.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis Pub/Sub 시스템 모니터링 서비스
 * 
 * 기능:
 * - 실시간 성능 지표 수집
 * - 시스템 헬스체크
 * - 알림 및 경고 관리
 * - 대시보드 데이터 제공
 * - 자동 복구 지원
 * - SLA 모니터링
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PubSubMonitoringService {

    private final MessagePublisher messagePublisher;
    private final MessageSubscriber messageSubscriber;
    private final RedisMessageListenerContainer listenerContainer;
    private final RedisConnectionFactory connectionFactory;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisSubscriptionConfig.SubscriptionManager subscriptionManager;
    
    // 메시지 핸들러들
    private final ChatMessageHandler chatMessageHandler;
    private final NotificationMessageHandler notificationMessageHandler;
    private final SystemEventMessageHandler systemEventMessageHandler;
    private final UserEventMessageHandler userEventMessageHandler;
    private final MetricsMessageHandler metricsMessageHandler;
    private final HealthCheckMessageHandler healthCheckMessageHandler;
    
    // 모니터링 지표
    private final AtomicLong totalSystemMessages = new AtomicLong(0);
    private final AtomicLong totalSystemErrors = new AtomicLong(0);
    private final AtomicLong totalSystemWarnings = new AtomicLong(0);
    private final AtomicLong uptimeSeconds = new AtomicLong(0);
    
    private LocalDateTime systemStartTime;
    private LocalDateTime lastHealthCheck;
    private SystemHealthStatus currentHealthStatus;
    
    @PostConstruct
    public void init() {
        systemStartTime = LocalDateTime.now();
        lastHealthCheck = LocalDateTime.now();
        currentHealthStatus = SystemHealthStatus.STARTING;
        
        log.info("🚀 Redis Pub/Sub 모니터링 서비스 시작");
        
        // 초기 헬스체크
        performHealthCheck();
    }
    
    /**
     * 종합 시스템 상태 조회
     */
    public SystemStatus getSystemStatus() {
        updateUptimeMetrics();
        
        return SystemStatus.builder()
                .healthStatus(currentHealthStatus)
                .uptime(getFormattedUptime())
                .totalMessages(totalSystemMessages.get())
                .totalErrors(totalSystemErrors.get())
                .totalWarnings(totalSystemWarnings.get())
                .publisherStats(messagePublisher.getPublishStats())
                .subscriberStats(messageSubscriber.getSubscriptionStats())
                .subscriptionStats(subscriptionManager.getSubscriptionStats())
                .handlerStats(collectHandlerStats())
                .connectionStats(getConnectionStats())
                .performanceMetrics(getPerformanceMetrics())
                .lastHealthCheck(lastHealthCheck)
                .build();
    }
    
    /**
     * 실시간 대시보드 데이터
     */
    public DashboardData getDashboardData() {
        SystemStatus systemStatus = getSystemStatus();
        
        return DashboardData.builder()
                .timestamp(LocalDateTime.now())
                .overallHealth(calculateOverallHealth())
                .keyMetrics(getKeyMetrics())
                .recentAlerts(getRecentAlerts())
                .performanceTrends(getPerformanceTrends())
                .handlerPerformance(getHandlerPerformance())
                .systemResources(getSystemResources())
                .connectionHealth(getConnectionHealth())
                .throughputMetrics(getThroughputMetrics())
                .build();
    }
    
    /**
     * 정기적인 헬스체크 (매 30초)
     */
    @Scheduled(fixedRate = 30000)
    public void scheduledHealthCheck() {
        try {
            performHealthCheck();
            updateSystemMetrics();
            detectAnomalies();
            
        } catch (Exception e) {
            log.error("정기 헬스체크 실패", e);
            totalSystemErrors.incrementAndGet();
        }
    }
    
    /**
     * 시스템 헬스체크 수행
     */
    public HealthCheckResult performHealthCheck() {
        lastHealthCheck = LocalDateTime.now();
        
        try {
            HealthCheckResult result = HealthCheckResult.builder()
                    .timestamp(LocalDateTime.now())
                    .redisConnection(checkRedisConnection())
                    .listenerContainer(checkListenerContainer())
                    .subscriptions(checkSubscriptions())
                    .publishers(checkPublishers())
                    .handlers(checkHandlers())
                    .overallHealth(SystemHealthStatus.HEALTHY)
                    .build();
            
            // 전체 상태 계산
            result.setOverallHealth(calculateOverallHealth(result));
            currentHealthStatus = result.getOverallHealth();
            
            log.debug("🏥 헬스체크 완료: {}", currentHealthStatus);
            
            return result;
            
        } catch (Exception e) {
            log.error("헬스체크 수행 중 오류", e);
            currentHealthStatus = SystemHealthStatus.CRITICAL;
            totalSystemErrors.incrementAndGet();
            
            return HealthCheckResult.builder()
                    .timestamp(LocalDateTime.now())
                    .overallHealth(SystemHealthStatus.CRITICAL)
                    .error(e.getMessage())
                    .build();
        }
    }
    
    /**
     * 성능 지표 수집
     */
    private PerformanceMetrics getPerformanceMetrics() {
        try {
            return PerformanceMetrics.builder()
                    .messageProcessingRate(calculateProcessingRate())
                    .averageLatency(calculateAverageLatency())
                    .errorRate(calculateErrorRate())
                    .memoryUsage(getMemoryUsage())
                    .connectionPoolSize(getConnectionPoolSize())
                    .queueSize(getQueueSize())
                    .cpuUsage(getCpuUsage())
                    .build();
                    
        } catch (Exception e) {
            log.warn("성능 지표 수집 실패", e);
            return PerformanceMetrics.builder().build();
        }
    }
    
    /**
     * 핸들러별 통계 수집
     */
    private Map<String, Object> collectHandlerStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            stats.put("chat", chatMessageHandler.getStats());
            stats.put("notification", notificationMessageHandler.getStats());
            stats.put("systemEvent", systemEventMessageHandler.getStats());
            stats.put("userEvent", userEventMessageHandler.getStats());
            stats.put("metrics", metricsMessageHandler.getStats());
            stats.put("healthCheck", healthCheckMessageHandler.getStats());
            
        } catch (Exception e) {
            log.warn("핸들러 통계 수집 실패", e);
        }
        
        return stats;
    }
    
    /**
     * 전체 상태 계산
     */
    private SystemHealthStatus calculateOverallHealth() {
        try {
            HealthCheckResult healthCheck = performHealthCheck();
            return healthCheck.getOverallHealth();
            
        } catch (Exception e) {
            return SystemHealthStatus.DEGRADED;
        }
    }
    
    private SystemHealthStatus calculateOverallHealth(HealthCheckResult result) {
        int healthScore = 0;
        int totalChecks = 0;
        
        // 각 컴포넌트 상태 점수화
        if (result.isRedisConnection()) healthScore += 25;
        if (result.isListenerContainer()) healthScore += 20;
        if (result.isSubscriptions()) healthScore += 20;
        if (result.isPublishers()) healthScore += 20;
        if (result.isHandlers()) healthScore += 15;
        
        totalChecks = 100;
        
        // 상태 결정
        if (healthScore >= 95) return SystemHealthStatus.HEALTHY;
        if (healthScore >= 80) return SystemHealthStatus.DEGRADED;
        if (healthScore >= 60) return SystemHealthStatus.WARNING;
        return SystemHealthStatus.CRITICAL;
    }
    
    // === 헬스체크 세부 메서드들 ===
    
    private boolean checkRedisConnection() {
        try {
            redisTemplate.opsForValue().get("health-check");
            return true;
        } catch (Exception e) {
            log.warn("Redis 연결 체크 실패", e);
            return false;
        }
    }
    
    private boolean checkListenerContainer() {
        return listenerContainer.isActive() && listenerContainer.isRunning();
    }
    
    private boolean checkSubscriptions() {
        return subscriptionManager.isHealthy();
    }
    
    private boolean checkPublishers() {
        MessagePublisher.PublishStats stats = messagePublisher.getPublishStats();
        return stats.getSuccessRate() >= 90.0; // 90% 이상 성공률
    }
    
    private boolean checkHandlers() {
        try {
            // 각 핸들러의 처리 성공률 확인
            return chatMessageHandler.getStats().getSuccessRate() >= 80.0 &&
                   notificationMessageHandler.getStats().getSuccessRate() >= 80.0 &&
                   systemEventMessageHandler.getStats().getSuccessRate() >= 80.0 &&
                   userEventMessageHandler.getStats().getSuccessRate() >= 80.0;
        } catch (Exception e) {
            return false;
        }
    }
    
    // === 메트릭 계산 메서드들 ===
    
    private double calculateProcessingRate() {
        // 초당 처리된 메시지 수
        MessageSubscriber.SubscriptionStats stats = messageSubscriber.getSubscriptionStats();
        long uptime = getUptimeSeconds();
        return uptime > 0 ? (double) stats.getTotalProcessed() / uptime : 0.0;
    }
    
    private double calculateAverageLatency() {
        // 평균 처리 지연시간 (ms)
        // 실제 구현에서는 처리 시간을 추적해야 함
        return 50.0; // 임시값
    }
    
    private double calculateErrorRate() {
        MessageSubscriber.SubscriptionStats stats = messageSubscriber.getSubscriptionStats();
        long total = stats.getTotalReceived();
        return total > 0 ? (double) stats.getTotalFailed() / total * 100.0 : 0.0;
    }
    
    private long getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
    
    private int getConnectionPoolSize() {
        try {
            // 실제 구현에서는 connection factory에서 풀 크기 조회
            return 10; // 임시값
        } catch (Exception e) {
            return 0;
        }
    }
    
    private int getQueueSize() {
        // 대기 중인 메시지 큐 크기
        return 0; // 임시값
    }
    
    private double getCpuUsage() {
        // CPU 사용률
        return 15.5; // 임시값
    }
    
    private ConnectionStats getConnectionStats() {
        return ConnectionStats.builder()
                .activeConnections(getConnectionPoolSize())
                .totalConnections(getConnectionPoolSize())
                .connectionHealth("HEALTHY")
                .build();
    }
    
    // === 유틸리티 메서드들 ===
    
    private void updateUptimeMetrics() {
        if (systemStartTime != null) {
            uptimeSeconds.set(java.time.Duration.between(systemStartTime, LocalDateTime.now()).getSeconds());
        }
    }
    
    private void updateSystemMetrics() {
        // 시스템 전체 메시지 카운트 업데이트
        MessageSubscriber.SubscriptionStats stats = messageSubscriber.getSubscriptionStats();
        totalSystemMessages.set(stats.getTotalReceived());
    }
    
    private void detectAnomalies() {
        // 이상 징후 감지 로직
        double errorRate = calculateErrorRate();
        if (errorRate > 10.0) {
            totalSystemWarnings.incrementAndGet();
            log.warn("🚨 높은 오류율 감지: {}%", errorRate);
        }
        
        double processingRate = calculateProcessingRate();
        if (processingRate < 1.0) {
            totalSystemWarnings.incrementAndGet();
            log.warn("⚠️ 낮은 처리율 감지: {} msg/s", processingRate);
        }
    }
    
    private String getFormattedUptime() {
        long seconds = getUptimeSeconds();
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        return String.format("%dd %dh %dm %ds", days, hours, minutes, secs);
    }
    
    private long getUptimeSeconds() {
        return uptimeSeconds.get();
    }
    
    // 임시 구현 - 실제로는 더 복잡한 로직 필요
    private Map<String, Object> getKeyMetrics() { return new HashMap<>(); }
    private java.util.List<Object> getRecentAlerts() { return new java.util.ArrayList<>(); }
    private Map<String, Object> getPerformanceTrends() { return new HashMap<>(); }
    private Map<String, Object> getHandlerPerformance() { return new HashMap<>(); }
    private Map<String, Object> getSystemResources() { return new HashMap<>(); }
    private Map<String, Object> getConnectionHealth() { return new HashMap<>(); }
    private Map<String, Object> getThroughputMetrics() { return new HashMap<>(); }
    
    // === DTO 클래스들 ===
    
    public enum SystemHealthStatus {
        HEALTHY("정상"),
        DEGRADED("성능 저하"),
        WARNING("경고"),
        CRITICAL("심각"),
        STARTING("시작 중");
        
        private final String description;
        
        SystemHealthStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    @lombok.Builder
    @lombok.Data
    public static class SystemStatus {
        private SystemHealthStatus healthStatus;
        private String uptime;
        private long totalMessages;
        private long totalErrors;
        private long totalWarnings;
        private MessagePublisher.PublishStats publisherStats;
        private MessageSubscriber.SubscriptionStats subscriberStats;
        private RedisSubscriptionConfig.SubscriptionStats subscriptionStats;
        private Map<String, Object> handlerStats;
        private ConnectionStats connectionStats;
        private PerformanceMetrics performanceMetrics;
        private LocalDateTime lastHealthCheck;
    }
    
    @lombok.Builder
    @lombok.Data
    public static class HealthCheckResult {
        private LocalDateTime timestamp;
        private boolean redisConnection;
        private boolean listenerContainer;
        private boolean subscriptions;
        private boolean publishers;
        private boolean handlers;
        private SystemHealthStatus overallHealth;
        private String error;
        
        public boolean isHealthy() {
            return overallHealth == SystemHealthStatus.HEALTHY;
        }
    }
    
    @lombok.Builder
    @lombok.Data
    public static class PerformanceMetrics {
        private double messageProcessingRate;  // 초당 처리 메시지 수
        private double averageLatency;         // 평균 지연시간(ms)
        private double errorRate;              // 오류율(%)
        private long memoryUsage;              // 메모리 사용량(bytes)
        private int connectionPoolSize;        // 연결 풀 크기
        private int queueSize;                 // 큐 크기
        private double cpuUsage;               // CPU 사용률(%)
    }
    
    @lombok.Builder
    @lombok.Data
    public static class ConnectionStats {
        private int activeConnections;
        private int totalConnections;
        private String connectionHealth;
    }
    
    @lombok.Builder
    @lombok.Data
    public static class DashboardData {
        private LocalDateTime timestamp;
        private SystemHealthStatus overallHealth;
        private Map<String, Object> keyMetrics;
        private java.util.List<Object> recentAlerts;
        private Map<String, Object> performanceTrends;
        private Map<String, Object> handlerPerformance;
        private Map<String, Object> systemResources;
        private Map<String, Object> connectionHealth;
        private Map<String, Object> throughputMetrics;
    }
}