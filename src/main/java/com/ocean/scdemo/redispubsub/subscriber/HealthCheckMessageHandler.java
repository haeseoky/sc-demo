package com.ocean.scdemo.redispubsub.subscriber;

import com.ocean.scdemo.redispubsub.message.HealthCheckMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 헬스체크 메시지 처리 핸들러
 * 
 * 기능:
 * - 서비스 상태 모니터링
 * - 데이터베이스 연결 상태 추적
 * - 네트워크 상태 모니터링
 * - 자동 복구 및 재시작
 * - 서비스 의존성 추적
 * - 헬스체크 실패 알림
 */
@Slf4j
@Component
public class HealthCheckMessageHandler {

    // 처리 통계
    private final AtomicLong totalProcessed = new AtomicLong(0);
    private final AtomicLong healthyServices = new AtomicLong(0);
    private final AtomicLong unhealthyServices = new AtomicLong(0);
    private final AtomicLong degradedServices = new AtomicLong(0);
    private final AtomicLong databaseChecks = new AtomicLong(0);
    private final AtomicLong externalApiChecks = new AtomicLong(0);
    private final AtomicLong autoRecoveries = new AtomicLong(0);
    
    // 서비스 상태 추적
    private final Map<String, ServiceHealthStatus> serviceStatuses = new ConcurrentHashMap<>();
    
    // 실패 빈도 추적
    private final Map<String, HealthFailureTracker> failureTrackers = new ConcurrentHashMap<>();
    
    public boolean handleMessage(HealthCheckMessage message) {
        try {
            totalProcessed.incrementAndGet();
            
            // 체크 타입별 처리
            boolean processed = switch (message.getCheckType()) {
                case "SERVICE" -> handleServiceHealthCheck(message);
                case "DATABASE" -> handleDatabaseHealthCheck(message);
                case "CACHE" -> handleCacheHealthCheck(message);
                case "EXTERNAL_API" -> handleExternalApiHealthCheck(message);
                case "DISK" -> handleDiskHealthCheck(message);
                case "MEMORY" -> handleMemoryHealthCheck(message);
                default -> handleGenericHealthCheck(message);
            };
            
            if (processed) {
                // 서비스 상태 업데이트
                updateServiceStatus(message);
                
                // 실패 추적 및 복구 처리
                handleFailureTracking(message);
                
                // 알림 처리
                processHealthAlert(message);
                
                log.debug("헬스체크 처리 완료: 서비스={}, 상태={}, 타입={}, 점수={}", 
                         message.getServiceName(), message.getStatus(), message.getCheckType(), message.getHealthScore());
            }
            
            return processed;
            
        } catch (Exception e) {
            log.error("헬스체크 처리 실패: ID={}", message.getMessageId(), e);
            return false;
        }
    }
    
    /**
     * 서비스 헬스체크 처리
     */
    private boolean handleServiceHealthCheck(HealthCheckMessage message) {
        String statusIcon = getStatusIcon(message.getStatus());
        
        log.info("{} 서비스 헬스체크: 서비스={}, 상태={}, 응답시간={}ms, 점수={}", 
                statusIcon, message.getServiceName(), message.getStatus(), 
                message.getResponseTime(), message.getHealthScore());
        
        // 상태에 따른 처리
        if (message.isUp()) {
            healthyServices.incrementAndGet();
            handleHealthyService(message);
        } else if (message.isDown()) {
            unhealthyServices.incrementAndGet();
            handleUnhealthyService(message);
        } else if (message.isDegraded()) {
            degradedServices.incrementAndGet();
            handleDegradedService(message);
        }
        
        return true;
    }
    
    /**
     * 데이터베이스 헬스체크 처리
     */
    private boolean handleDatabaseHealthCheck(HealthCheckMessage message) {
        databaseChecks.incrementAndGet();
        
        HealthCheckMessage.DatabaseHealth dbHealth = message.getDatabaseHealth();
        if (dbHealth != null) {
            log.info("💾 DB 헬스체크: 서비스={}, 연결={}, 연결시간={}ms, 활성연결={}/{}", 
                    message.getServiceName(), 
                    dbHealth.getConnected() ? "성공" : "실패", 
                    dbHealth.getConnectionTime(),
                    dbHealth.getActiveConnections(), 
                    dbHealth.getMaxConnections());
            
            // 데이터베이스 연결 실패 처리
            if (!dbHealth.getConnected()) {
                handleDatabaseConnectionFailure(message);
            }
            
            // 연결 풀 포화 상태 체크
            checkConnectionPoolSaturation(dbHealth);
        }
        
        return true;
    }
    
    /**
     * 캐시 헬스체크 처리
     */
    private boolean handleCacheHealthCheck(HealthCheckMessage message) {
        HealthCheckMessage.CacheHealth cacheHealth = message.getCacheHealth();
        if (cacheHealth != null) {
            log.info("💾 캐시 헬스체크: 서비스={}, 연결={}, 히트율={}%, 메모리={}/{} MB", 
                    message.getServiceName(), 
                    cacheHealth.getConnected() ? "성공" : "실패",
                    cacheHealth.getHitRate(),
                    cacheHealth.getUsedMemory() / 1024 / 1024,
                    cacheHealth.getMaxMemory() / 1024 / 1024);
            
            // 캐시 성능 분석
            analyzeCachePerformance(cacheHealth);
        }
        
        return true;
    }
    
    /**
     * 외부 API 헬스체크 처리
     */
    private boolean handleExternalApiHealthCheck(HealthCheckMessage message) {
        externalApiChecks.incrementAndGet();
        
        if (message.getExternalApiHealths() != null) {
            for (HealthCheckMessage.ExternalApiHealth apiHealth : message.getExternalApiHealths()) {
                log.info("🌐 외부 API 체크: API={}, 상태={}, 응답시간={}ms, 상태코드={}", 
                        apiHealth.getApiName(), 
                        apiHealth.getAvailable() ? "UP" : "DOWN",
                        apiHealth.getResponseTime(),
                        apiHealth.getStatusCode());
                
                // API 실패 처리
                if (!apiHealth.getAvailable()) {
                    handleExternalApiFailure(apiHealth, message);
                }
            }
        }
        
        return true;
    }
    
    /**
     * 디스크 헬스체크 처리
     */
    private boolean handleDiskHealthCheck(HealthCheckMessage message) {
        log.info("💾 디스크 체크: 호스트={}, 사용량={}%, 여유공간={} GB", 
                message.getHostName(), 
                message.getDiskUsage(),
                message.getFreeDisk() != null ? message.getFreeDisk() / 1024 / 1024 / 1024 : "unknown");
        
        // 디스크 공간 부족 알림
        if (message.getDiskUsage() != null && message.getDiskUsage() > 90.0) {
            log.error("🟥 디스크 공간 부족: {}% 사용 중", message.getDiskUsage());
            triggerDiskSpaceAlert(message);
        }
        
        return true;
    }
    
    /**
     * 메모리 헬스체크 처리
     */
    private boolean handleMemoryHealthCheck(HealthCheckMessage message) {
        log.info("💾 메모리 체크: 호스트={}, 사용량={}%, 여유메모리={} MB", 
                message.getHostName(),
                message.getMemoryUsage(),
                message.getFreeMemory() != null ? message.getFreeMemory() / 1024 / 1024 : "unknown");
        
        // 메모리 부족 경고
        if (message.getMemoryUsage() != null && message.getMemoryUsage() > 85.0) {
            log.warn("🟡 메모리 부족: {}% 사용 중", message.getMemoryUsage());
            triggerMemoryAlert(message);
        }
        
        return true;
    }
    
    /**
     * 일반 헬스체크 처리
     */
    private boolean handleGenericHealthCheck(HealthCheckMessage message) {
        log.debug("🩺 일반 헬스체크: 타입={}, 서비스={}, 상태={}", 
                 message.getCheckType(), message.getServiceName(), message.getStatus());
        
        return true;
    }
    
    // === 상태별 세부 처리 ===
    
    private void handleHealthyService(HealthCheckMessage message) {
        log.debug("✅ 건강한 서비스: {}", message.getServiceName());
        
        // 이전에 비건강 상태였던 경우 복구 알림
        ServiceHealthStatus previousStatus = serviceStatuses.get(message.getServiceName());
        if (previousStatus != null && !"건강".equals(previousStatus.getStatus())) {
            log.info("🎉 서비스 복구: {} ({} -> 건강)", 
                    message.getServiceName(), previousStatus.getStatus());
            sendRecoveryNotification(message);
        }
    }
    
    private void handleUnhealthyService(HealthCheckMessage message) {
        log.error("❌ 비건강 서비스: {} - {}", 
                 message.getServiceName(), message.getHealthDetails());
        
        // 자동 복구 시도
        if (shouldAttemptRecovery(message)) {
            attemptAutoRecovery(message);
        }
        
        // 실패 알림
        sendFailureAlert(message);
    }
    
    private void handleDegradedService(HealthCheckMessage message) {
        log.warn("🟡 성능 저하 서비스: {} - 점수: {}", 
                message.getServiceName(), message.getHealthScore());
        
        // 성능 개선 액션 트리거
        triggerPerformanceImprovement(message);
    }
    
    // === 실패 처리 및 복구 ===
    
    private void handleFailureTracking(HealthCheckMessage message) {
        String serviceKey = message.getServiceName() + ":" + message.getCheckType();
        
        if (!message.isHealthy()) {
            HealthFailureTracker tracker = failureTrackers.computeIfAbsent(serviceKey, 
                k -> new HealthFailureTracker());
            
            tracker.recordFailure();
            
            // 연속 실패 체크
            if (tracker.getConsecutiveFailures() >= 3) {
                log.error("🚨 연속 실패 감지: {} - {}+번 실패", 
                         serviceKey, tracker.getConsecutiveFailures());
                triggerCriticalAlert(message, tracker);
            }
        } else {
            // 성공 시 실패 카운트 리셋
            HealthFailureTracker tracker = failureTrackers.get(serviceKey);
            if (tracker != null) {
                tracker.resetConsecutiveFailures();
            }
        }
    }
    
    private boolean shouldAttemptRecovery(HealthCheckMessage message) {
        String serviceKey = message.getServiceName() + ":" + message.getCheckType();
        HealthFailureTracker tracker = failureTrackers.get(serviceKey);
        
        // 자동 복구 시도 조건
        return tracker != null && 
               tracker.getConsecutiveFailures() >= 2 && 
               tracker.getRecoveryAttempts() < 3; // 최대 3번 시도
    }
    
    private void attemptAutoRecovery(HealthCheckMessage message) {
        autoRecoveries.incrementAndGet();
        
        log.info("🔄 자동 복구 시도: 서비스={}, 타입={}", 
                message.getServiceName(), message.getCheckType());
        
        // 복구 시도 기록
        String serviceKey = message.getServiceName() + ":" + message.getCheckType();
        HealthFailureTracker tracker = failureTrackers.get(serviceKey);
        if (tracker != null) {
            tracker.incrementRecoveryAttempts();
        }
        
        // 복구 전략에 따른 다른 액션
        switch (message.getCheckType()) {
            case "SERVICE" -> restartService(message);
            case "DATABASE" -> reconnectDatabase(message);
            case "CACHE" -> clearCacheAndReconnect(message);
            case "EXTERNAL_API" -> retryExternalApi(message);
            default -> performGenericRecovery(message);
        }
    }
    
    // === 서비스 상태 관리 ===
    
    private void updateServiceStatus(HealthCheckMessage message) {
        ServiceHealthStatus status = ServiceHealthStatus.builder()
                .serviceName(message.getServiceName())
                .status(message.isHealthy() ? "건강" : (
                       message.isDegraded() ? "성능저하" : "비건강"))
                .lastCheckTime(LocalDateTime.now())
                .healthScore(message.getHealthScore())
                .responseTime(message.getResponseTime())
                .checkType(message.getCheckType())
                .build();
        
        serviceStatuses.put(message.getServiceName(), status);
    }
    
    // === 알림 처리 ===
    
    private void processHealthAlert(HealthCheckMessage message) {
        if (message.needsAlert()) {
            String alertLevel = message.getAlertLevel() != null ? 
                              message.getAlertLevel() : "MEDIUM";
            
            log.info("📬 헬스체크 알림: 서비스={}, 레벨={}", 
                    message.getServiceName(), alertLevel);
        }
    }
    
    // === 유틸리티 메서드들 ===
    
    private String getStatusIcon(String status) {
        return switch (status) {
            case "UP" -> "✅";
            case "DOWN" -> "❌";
            case "DEGRADED" -> "🟡";
            default -> "❔";
        };
    }
    
    private void handleDatabaseConnectionFailure(HealthCheckMessage message) {
        log.error("💾 DB 연결 실패: {}", message.getServiceName());
        // DB 연결 재시도 로직
    }
    
    private void checkConnectionPoolSaturation(HealthCheckMessage.DatabaseHealth dbHealth) {
        if (dbHealth.getActiveConnections() != null && dbHealth.getMaxConnections() != null) {
            double utilization = (double) dbHealth.getActiveConnections() / dbHealth.getMaxConnections();
            if (utilization > 0.9) { // 90% 초과
                log.warn("🟠 연결 풀 포화 상태: {}%", utilization * 100);
            }
        }
    }
    
    private void analyzeCachePerformance(HealthCheckMessage.CacheHealth cacheHealth) {
        if (cacheHealth.getHitRate() != null && cacheHealth.getHitRate() < 80) {
            log.warn("🟡 낮은 캐시 히트율: {}%", cacheHealth.getHitRate());
        }
    }
    
    private void handleExternalApiFailure(HealthCheckMessage.ExternalApiHealth apiHealth, 
                                         HealthCheckMessage message) {
        log.error("🌐 외부 API 실패: {} - {}", 
                 apiHealth.getApiName(), apiHealth.getErrorMessage());
    }
    
    private void triggerDiskSpaceAlert(HealthCheckMessage message) {
        // 디스크 공간 부족 알림
    }
    
    private void triggerMemoryAlert(HealthCheckMessage message) {
        // 메모리 부족 알림
    }
    
    private void sendRecoveryNotification(HealthCheckMessage message) {
        // 복구 알림 전송
    }
    
    private void sendFailureAlert(HealthCheckMessage message) {
        // 실패 알림 전송
    }
    
    private void triggerPerformanceImprovement(HealthCheckMessage message) {
        // 성능 개선 액션
    }
    
    private void triggerCriticalAlert(HealthCheckMessage message, HealthFailureTracker tracker) {
        // 심각한 알림 전송
    }
    
    private void restartService(HealthCheckMessage message) {
        // 서비스 재시작
    }
    
    private void reconnectDatabase(HealthCheckMessage message) {
        // DB 재연결
    }
    
    private void clearCacheAndReconnect(HealthCheckMessage message) {
        // 캐시 정리 및 재연결
    }
    
    private void retryExternalApi(HealthCheckMessage message) {
        // 외부 API 재시도
    }
    
    private void performGenericRecovery(HealthCheckMessage message) {
        // 일반적인 복구 액션
    }
    
    /**
     * 핸들러 통계 조회
     */
    public HealthCheckHandlerStats getStats() {
        return HealthCheckHandlerStats.builder()
                .totalProcessed(totalProcessed.get())
                .healthyServices(healthyServices.get())
                .unhealthyServices(unhealthyServices.get())
                .degradedServices(degradedServices.get())
                .databaseChecks(databaseChecks.get())
                .externalApiChecks(externalApiChecks.get())
                .autoRecoveries(autoRecoveries.get())
                .activeServices(serviceStatuses.size())
                .failureTrackers(failureTrackers.size())
                .build();
    }
    
    // === DTO 클래스들 ===
    
    @lombok.Builder
    @lombok.Data
    public static class ServiceHealthStatus {
        private String serviceName;
        private String status;
        private LocalDateTime lastCheckTime;
        private Integer healthScore;
        private Long responseTime;
        private String checkType;
    }
    
    @lombok.Data
    public static class HealthFailureTracker {
        private int consecutiveFailures = 0;
        private int totalFailures = 0;
        private int recoveryAttempts = 0;
        private LocalDateTime firstFailure;
        private LocalDateTime lastFailure;
        
        public void recordFailure() {
            consecutiveFailures++;
            totalFailures++;
            lastFailure = LocalDateTime.now();
            if (firstFailure == null) {
                firstFailure = LocalDateTime.now();
            }
        }
        
        public void resetConsecutiveFailures() {
            consecutiveFailures = 0;
        }
        
        public void incrementRecoveryAttempts() {
            recoveryAttempts++;
        }
    }
    
    @lombok.Builder
    @lombok.Data
    public static class HealthCheckHandlerStats {
        private final long totalProcessed;
        private final long healthyServices;
        private final long unhealthyServices;
        private final long degradedServices;
        private final long databaseChecks;
        private final long externalApiChecks;
        private final long autoRecoveries;
        private final int activeServices;
        private final int failureTrackers;
        
        public double getHealthyRate() {
            long totalServices = healthyServices + unhealthyServices + degradedServices;
            return totalServices > 0 ? 
                   ((double) healthyServices / totalServices) * 100.0 : 0.0;
        }
        
        public double getRecoverySuccessRate() {
            return totalProcessed > 0 ? 
                   ((double) autoRecoveries / totalProcessed) * 100.0 : 0.0;
        }
    }
}