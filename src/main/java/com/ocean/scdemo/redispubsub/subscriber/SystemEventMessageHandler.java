package com.ocean.scdemo.redispubsub.subscriber;

import com.ocean.scdemo.redispubsub.message.SystemEventMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 시스템 이벤트 메시지 처리 핸들러
 * 
 * 기능:
 * - 서비스 시작/종료 이벤트 처리
 * - 에러 및 예외 이벤트 처리
 * - 성능 지표 모니터링
 * - 시스템 알림 및 에스케이레이션
 * - 로그 집계 및 상관관계 분석
 * - 자동 복구 및 대응 액션
 */
@Slf4j
@Component
public class SystemEventMessageHandler {

    // 처리 통계
    private final AtomicLong totalProcessed = new AtomicLong(0);
    private final AtomicLong serverEvents = new AtomicLong(0);
    private final AtomicLong errorEvents = new AtomicLong(0);
    private final AtomicLong performanceEvents = new AtomicLong(0);
    private final AtomicLong deploymentEvents = new AtomicLong(0);
    private final AtomicLong alertEvents = new AtomicLong(0);
    private final AtomicLong criticalEvents = new AtomicLong(0);
    
    // 서비스 상태 추적
    private final Map<String, ServiceStatus> serviceStatuses = new ConcurrentHashMap<>();
    
    // 에러 빈도 추적 (last 5 minutes)
    private final Map<String, ErrorFrequency> errorFrequencies = new ConcurrentHashMap<>();
    
    public boolean handleMessage(SystemEventMessage message) {
        try {
            totalProcessed.incrementAndGet();
            
            // 이벤트 타입별 처리
            boolean processed = switch (message.getEventType()) {
                case "SERVER_START" -> handleServerStartEvent(message);
                case "SERVER_STOP" -> handleServerStopEvent(message);
                case "ERROR" -> handleErrorEvent(message);
                case "PERFORMANCE_ALERT" -> handlePerformanceAlert(message);
                case "DEPLOYMENT" -> handleDeploymentEvent(message);
                case "HEALTH_CHECK_FAILED" -> handleHealthCheckFailed(message);
                default -> handleGenericSystemEvent(message);
            };
            
            if (processed) {
                // 서비스 상태 업데이트
                updateServiceStatus(message);
                
                // 상관관계 추적
                trackCorrelation(message);
                
                // 에스케이레이션 처리
                handleEscalation(message);
                
                log.debug("시스템 이벤트 처리 완료: 서비스={}, 이벤트={}, 레벨={}", 
                         message.getServiceName(), message.getEventType(), message.getEventLevel());
            }
            
            return processed;
            
        } catch (Exception e) {
            log.error("시스템 이벤트 처리 실패: ID={}", message.getMessageId(), e);
            return false;
        }
    }
    
    /**
     * 서뺄 시작 이벤트 처리
     */
    private boolean handleServerStartEvent(SystemEventMessage message) {
        serverEvents.incrementAndGet();
        
        log.info("🚀 서뺄 시작: 서비스={}, 인스턴스={}, 버전={}", 
                message.getServiceName(), message.getInstanceId(), message.getServiceVersion());
        
        // 서비스 상태를 STARTING으로 설정
        setServiceStatus(message.getServiceName(), "STARTING", message);
        
        // 시작 시간 기록
        recordServiceStartTime(message.getServiceName(), message.getEventStartTime());
        
        // 다른 서비스들에게 시작 알림
        notifyServiceStart(message);
        
        return true;
    }
    
    /**
     * 서뺄 종료 이벤트 처리
     */
    private boolean handleServerStopEvent(SystemEventMessage message) {
        serverEvents.incrementAndGet();
        
        log.info("🛑 서뺄 종료: 서비스={}, 인스턴스={}, 지속시간={}초", 
                message.getServiceName(), message.getInstanceId(), 
                message.getDuration() != null ? message.getDuration() / 1000 : "unknown");
        
        // 서비스 상태를 STOPPED로 설정
        setServiceStatus(message.getServiceName(), "STOPPED", message);
        
        // Graceful shutdown인지 확인
        boolean graceful = message.getStatus() != null && "COMPLETED".equals(message.getStatus());
        if (!graceful) {
            log.warn("비정상 서비스 종료: 서비스={}", message.getServiceName());
            triggerServiceRecovery(message);
        }
        
        return true;
    }
    
    /**
     * 에러 이벤트 처리
     */
    private boolean handleErrorEvent(SystemEventMessage message) {
        errorEvents.incrementAndGet();
        
        log.error("❌ 시스템 에러: 서비스={}, 코드={}, 메시지='{}'", 
                 message.getServiceName(), message.getErrorCode(), message.getErrorMessage());
        
        // 에러 빈도 추적
        trackErrorFrequency(message.getServiceName(), message.getErrorCode());
        
        // 심각한 에러인지 판단
        if (isCriticalError(message)) {
            criticalEvents.incrementAndGet();
            handleCriticalError(message);
        }
        
        // 자동 복구 시도
        if (shouldAttemptAutoRecovery(message)) {
            scheduleAutoRecovery(message);
        }
        
        // 에러 패턴 분석
        analyzeErrorPattern(message);
        
        return true;
    }
    
    /**
     * 성능 알림 처리
     */
    private boolean handlePerformanceAlert(SystemEventMessage message) {
        performanceEvents.incrementAndGet();
        
        log.warn("📈 성능 알림: 서비스={}, 응답시간={}ms, CPU={}%, 메모리={}%", 
                message.getServiceName(), message.getResponseTime(), 
                message.getCpuUsage(), message.getMemoryUsage());
        
        // 성능 임계치 초과 확인
        boolean exceedsThreshold = checkPerformanceThresholds(message);
        
        if (exceedsThreshold) {
            // 자동 스케일링 또는 로드 밸런싱 조정
            triggerPerformanceOptimization(message);
            
            // 성능 저하 알림
            sendPerformanceAlert(message);
        }
        
        // 성능 데이터 수집
        collectPerformanceMetrics(message);
        
        return true;
    }
    
    /**
     * 배포 이벤트 처리
     */
    private boolean handleDeploymentEvent(SystemEventMessage message) {
        deploymentEvents.incrementAndGet();
        
        log.info("🚀 배포 이벤트: 서비스={}, 버전={}, 상태={}", 
                message.getServiceName(), message.getServiceVersion(), message.getStatus());
        
        // 배포 상태에 따른 처리
        return switch (message.getStatus()) {
            case "STARTED" -> handleDeploymentStart(message);
            case "IN_PROGRESS" -> handleDeploymentProgress(message);
            case "COMPLETED" -> handleDeploymentComplete(message);
            case "FAILED" -> handleDeploymentFailed(message);
            default -> handleGenericDeployment(message);
        };
    }
    
    /**
     * 헬스체크 실패 이벤트 처리
     */
    private boolean handleHealthCheckFailed(SystemEventMessage message) {
        log.error("🌡️ 헬스체크 실패: 서비스={}, 상세={}", 
                 message.getServiceName(), message.getDetails());
        
        // 서비스 상태를 UNHEALTHY로 설정
        setServiceStatus(message.getServiceName(), "UNHEALTHY", message);
        
        // 재시작 또는 트래픽 리다이렉션
        triggerServiceRecovery(message);
        
        return true;
    }
    
    /**
     * 일반 시스템 이벤트 처리
     */
    private boolean handleGenericSystemEvent(SystemEventMessage message) {
        log.info("🔧 시스템 이벤트: 서비스={}, 이벤트={}, 레벨={}", 
                message.getServiceName(), message.getEventType(), message.getEventLevel());
        
        // 기본 로깅 및 메트릭 수집
        return true;
    }
    
    // === 서비스 상태 관리 ===
    
    private void setServiceStatus(String serviceName, String status, SystemEventMessage message) {
        serviceStatuses.put(serviceName, ServiceStatus.builder()
                .serviceName(serviceName)
                .status(status)
                .lastUpdate(LocalDateTime.now())
                .instanceId(message.getInstanceId())
                .version(message.getServiceVersion())
                .build());
    }
    
    private void updateServiceStatus(SystemEventMessage message) {
        ServiceStatus current = serviceStatuses.get(message.getServiceName());
        if (current != null) {
            current.setLastEventTime(LocalDateTime.now());
            current.setLastEventType(message.getEventType());
        }
    }
    
    // === 에러 처리 ===
    
    private void trackErrorFrequency(String serviceName, String errorCode) {
        String key = serviceName + ":" + errorCode;
        errorFrequencies.computeIfAbsent(key, k -> new ErrorFrequency()).increment();
    }
    
    private boolean isCriticalError(SystemEventMessage message) {
        // 심각한 에러 판단 로직
        return "FATAL".equals(message.getEventLevel()) || 
               message.getErrorCode() != null && message.getErrorCode().startsWith("CRITICAL_");
    }
    
    private void handleCriticalError(SystemEventMessage message) {
        log.error("🚨 심각한 오류 발생: 서비스={}", message.getServiceName());
        
        // 즉시 알림
        sendCriticalAlert(message);
        
        // 서비스 격리 및 트래픽 차단
        isolateService(message.getServiceName());
    }
    
    private boolean shouldAttemptAutoRecovery(SystemEventMessage message) {
        // 자동 복구 시도 여부 결정
        ErrorFrequency frequency = errorFrequencies.get(
            message.getServiceName() + ":" + message.getErrorCode());
        
        return frequency != null && frequency.getCount() < 3; // 3회 미만
    }
    
    private void scheduleAutoRecovery(SystemEventMessage message) {
        log.info("🔄 자동 복구 시도: 서비스={}", message.getServiceName());
        
        // 실제로는 스케줄러에 복구 작업 등록
        // recoveryScheduler.schedule(() -> attemptRecovery(message), 30, SECONDS);
    }
    
    // === 성능 처리 ===
    
    private boolean checkPerformanceThresholds(SystemEventMessage message) {
        boolean exceedsThreshold = false;
        
        if (message.getResponseTime() != null && message.getResponseTime() > 5000) { // 5초
            exceedsThreshold = true;
        }
        
        if (message.getCpuUsage() != null && message.getCpuUsage() > 80.0) { // 80%
            exceedsThreshold = true;
        }
        
        if (message.getMemoryUsage() != null && message.getMemoryUsage() > 85.0) { // 85%
            exceedsThreshold = true;
        }
        
        return exceedsThreshold;
    }
    
    private void triggerPerformanceOptimization(SystemEventMessage message) {
        log.warn("🏃 성능 최적화 트리거: 서비스={}", message.getServiceName());
        
        // 자동 스케일링, 캐시 정리, 리소스 해제 등
    }
    
    // === 배포 처리 ===
    
    private boolean handleDeploymentStart(SystemEventMessage message) {
        log.info("🚀 배포 시작: {}", message.getServiceName());
        setServiceStatus(message.getServiceName(), "DEPLOYING", message);
        return true;
    }
    
    private boolean handleDeploymentProgress(SystemEventMessage message) {
        log.debug("📋 배포 진행: {}", message.getServiceName());
        return true;
    }
    
    private boolean handleDeploymentComplete(SystemEventMessage message) {
        log.info("✅ 배포 완료: {} v{}", message.getServiceName(), message.getServiceVersion());
        setServiceStatus(message.getServiceName(), "RUNNING", message);
        
        // 배포 후 헬스체크 실행
        schedulePostDeploymentHealthCheck(message);
        return true;
    }
    
    private boolean handleDeploymentFailed(SystemEventMessage message) {
        log.error("❌ 배포 실패: {}", message.getServiceName());
        setServiceStatus(message.getServiceName(), "DEPLOY_FAILED", message);
        
        // 롤백 시도
        triggerRollback(message);
        return true;
    }
    
    private boolean handleGenericDeployment(SystemEventMessage message) {
        log.info("🔧 배포 이벤트: {} - {}", message.getServiceName(), message.getStatus());
        return true;
    }
    
    // === 유틸리티 메서드들 ===
    
    private void trackCorrelation(SystemEventMessage message) {
        if (message.getCorrelationId() != null) {
            log.debug("상관관계 추적: 상관ID={}", message.getCorrelationId());
        }
    }
    
    private void handleEscalation(SystemEventMessage message) {
        if (message.needsEscalation()) {
            log.warn("🚨 에스케이레이션 필요: 서비스={}", message.getServiceName());
            escalateToManagement(message);
        }
    }
    
    private void recordServiceStartTime(String serviceName, LocalDateTime startTime) {
        // 서비스 시작 시간 기록 로직
    }
    
    private void notifyServiceStart(SystemEventMessage message) {
        // 다른 서비스들에 알림
    }
    
    private void triggerServiceRecovery(SystemEventMessage message) {
        // 서비스 복구 로직
    }
    
    private void analyzeErrorPattern(SystemEventMessage message) {
        // 에러 패턴 분석
    }
    
    private void sendPerformanceAlert(SystemEventMessage message) {
        // 성능 알림 전송
    }
    
    private void collectPerformanceMetrics(SystemEventMessage message) {
        // 성능 메트릭 수집
    }
    
    private void sendCriticalAlert(SystemEventMessage message) {
        // 심각 알림 전송
    }
    
    private void isolateService(String serviceName) {
        // 서비스 격리
    }
    
    private void schedulePostDeploymentHealthCheck(SystemEventMessage message) {
        // 배포 후 헬스체크
    }
    
    private void triggerRollback(SystemEventMessage message) {
        // 롤백 실행
    }
    
    private void escalateToManagement(SystemEventMessage message) {
        // 경영진 에스케이레이션
    }
    
    /**
     * 핸들러 통계 조회
     */
    public SystemEventHandlerStats getStats() {
        return SystemEventHandlerStats.builder()
                .totalProcessed(totalProcessed.get())
                .serverEvents(serverEvents.get())
                .errorEvents(errorEvents.get())
                .performanceEvents(performanceEvents.get())
                .deploymentEvents(deploymentEvents.get())
                .alertEvents(alertEvents.get())
                .criticalEvents(criticalEvents.get())
                .activeServices(serviceStatuses.size())
                .build();
    }
    
    // === DTO 클래스들 ===
    
    @lombok.Builder
    @lombok.Data
    public static class ServiceStatus {
        private String serviceName;
        private String status;
        private LocalDateTime lastUpdate;
        private String instanceId;
        private String version;
        private LocalDateTime lastEventTime;
        private String lastEventType;
    }
    
    @lombok.Data
    public static class ErrorFrequency {
        private int count = 0;
        private LocalDateTime firstOccurrence = LocalDateTime.now();
        private LocalDateTime lastOccurrence;
        
        public void increment() {
            count++;
            lastOccurrence = LocalDateTime.now();
        }
    }
    
    @lombok.Builder
    @lombok.Data
    public static class SystemEventHandlerStats {
        private final long totalProcessed;
        private final long serverEvents;
        private final long errorEvents;
        private final long performanceEvents;
        private final long deploymentEvents;
        private final long alertEvents;
        private final long criticalEvents;
        private final int activeServices;
        
        public double getErrorRate() {
            return totalProcessed > 0 ? 
                   ((double) errorEvents / totalProcessed) * 100.0 : 0.0;
        }
        
        public double getCriticalRate() {
            return errorEvents > 0 ? 
                   ((double) criticalEvents / errorEvents) * 100.0 : 0.0;
        }
        
        public double getSuccessRate() {
            return totalProcessed > 0 ? 
                   ((double) (totalProcessed - errorEvents) / totalProcessed) * 100.0 : 0.0;
        }
    }
}