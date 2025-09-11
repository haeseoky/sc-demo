package com.ocean.scdemo.redispubsub.monitoring;

import com.ocean.scdemo.redispubsub.config.RedisSubscriptionConfig;
import com.ocean.scdemo.redispubsub.publisher.MessagePublisher;
import com.ocean.scdemo.redispubsub.subscriber.MessageSubscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Redis Pub/Sub 모니터링 대시보드 컨트롤러
 * 
 * 기능:
 * - 실시간 모니터링 데이터 API
 * - 시스템 상태 조회
 * - 성능 메트릭스 조회
 * - 알림 관리
 * - 헬스체크 엔드포인트
 * - 설정 관리
 */
@Slf4j
@RestController
@RequestMapping("/api/pubsub/monitoring")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // 실제 운영 환경에서는 적절히 제한
public class MonitoringDashboardController {

    private final PubSubMonitoringService monitoringService;
    private final MetricsCollectorService metricsCollector;
    private final AlertingService alertingService;
    private final MessagePublisher messagePublisher;
    private final MessageSubscriber messageSubscriber;
    private final RedisSubscriptionConfig.SubscriptionManager subscriptionManager;
    
    // === 대시보드 메인 데이터 ===
    
    /**
     * 대시보드 메인 데이터 조회
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardData() {
        try {
            PubSubMonitoringService.DashboardData dashboardData = monitoringService.getDashboardData();
            PubSubMonitoringService.SystemStatus systemStatus = monitoringService.getSystemStatus();
            MetricsCollectorService.MetricsSnapshot metricsSnapshot = metricsCollector.getCurrentSnapshot();
            AlertingService.AlertingStats alertingStats = alertingService.getAlertingStats();
            
            Map<String, Object> response = new HashMap<>();
            response.put("timestamp", LocalDateTime.now());
            response.put("dashboard", dashboardData);
            response.put("system", systemStatus);
            response.put("metrics", metricsSnapshot);
            response.put("alerts", alertingStats);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("대시보드 데이터 조회 실패", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "대시보드 데이터 조회 중 오류가 발생했습니다"));
        }
    }
    
    // === 시스템 상태 관리 ===
    
    /**
     * 시스템 전체 상태 조회
     */
    @GetMapping("/system/status")
    public ResponseEntity<PubSubMonitoringService.SystemStatus> getSystemStatus() {
        try {
            PubSubMonitoringService.SystemStatus status = monitoringService.getSystemStatus();
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            log.error("시스템 상태 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 헬스체크 수행
     */
    @PostMapping("/system/health-check")
    public ResponseEntity<PubSubMonitoringService.HealthCheckResult> performHealthCheck() {
        try {
            PubSubMonitoringService.HealthCheckResult result = monitoringService.performHealthCheck();
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("헬스체크 실행 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 시스템 정보 조회
     */
    @GetMapping("/system/info")
    public ResponseEntity<Map<String, Object>> getSystemInfo() {
        try {
            Map<String, Object> info = new HashMap<>();
            
            // JVM 정보
            Runtime runtime = Runtime.getRuntime();
            info.put("jvm", Map.of(
                "totalMemory", runtime.totalMemory(),
                "freeMemory", runtime.freeMemory(),
                "usedMemory", runtime.totalMemory() - runtime.freeMemory(),
                "maxMemory", runtime.maxMemory(),
                "availableProcessors", runtime.availableProcessors()
            ));
            
            // 시스템 속성
            info.put("system", Map.of(
                "javaVersion", System.getProperty("java.version"),
                "osName", System.getProperty("os.name"),
                "osVersion", System.getProperty("os.version"),
                "osArch", System.getProperty("os.arch")
            ));
            
            // 애플리케이션 정보
            info.put("application", Map.of(
                "startTime", LocalDateTime.now(), // 실제로는 애플리케이션 시작 시간
                "uptime", "계산 필요"
            ));
            
            return ResponseEntity.ok(info);
            
        } catch (Exception e) {
            log.error("시스템 정보 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // === 메트릭스 관리 ===
    
    /**
     * 현재 메트릭스 스냅샷
     */
    @GetMapping("/metrics/snapshot")
    public ResponseEntity<MetricsCollectorService.MetricsSnapshot> getMetricsSnapshot() {
        try {
            MetricsCollectorService.MetricsSnapshot snapshot = metricsCollector.getCurrentSnapshot();
            return ResponseEntity.ok(snapshot);
            
        } catch (Exception e) {
            log.error("메트릭스 스냅샷 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 특정 메트릭 시계열 데이터 조회
     */
    @GetMapping("/metrics/timeseries/{metricName}")
    public ResponseEntity<TimeSeries> getTimeSeries(
            @PathVariable String metricName,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        try {
            TimeSeries series;
            
            if (from != null && to != null) {
                LocalDateTime fromTime = LocalDateTime.parse(from);
                LocalDateTime toTime = LocalDateTime.parse(to);
                series = metricsCollector.getTimeSeries(metricName, fromTime, toTime);
            } else {
                series = metricsCollector.getTimeSeries(metricName);
            }
            
            if (series == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(series);
            
        } catch (Exception e) {
            log.error("시계열 데이터 조회 실패: {}", metricName, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 메트릭 트렌드 분석
     */
    @GetMapping("/metrics/trends/{metricName}")
    public ResponseEntity<MetricsCollectorService.TrendAnalysis> getTrendAnalysis(
            @PathVariable String metricName,
            @RequestParam(defaultValue = "24") int hours) {
        try {
            MetricsCollectorService.TrendAnalysis analysis = 
                metricsCollector.analyzeTrends(metricName, hours);
            
            return ResponseEntity.ok(analysis);
            
        } catch (Exception e) {
            log.error("트렌드 분석 실패: {}", metricName, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 메트릭 데이터 내보내기
     */
    @GetMapping("/metrics/export")
    public ResponseEntity<Map<String, Object>> exportMetrics(
            @RequestParam(defaultValue = "JSON") String format) {
        try {
            MetricsCollectorService.ExportFormat exportFormat = 
                MetricsCollectorService.ExportFormat.valueOf(format.toUpperCase());
            
            Map<String, Object> exportData = metricsCollector.exportMetrics(exportFormat);
            
            return ResponseEntity.ok(exportData);
            
        } catch (Exception e) {
            log.error("메트릭 내보내기 실패", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    // === 알림 관리 ===
    
    /**
     * 알림 통계 조회
     */
    @GetMapping("/alerts/stats")
    public ResponseEntity<AlertingService.AlertingStats> getAlertingStats() {
        try {
            AlertingService.AlertingStats stats = alertingService.getAlertingStats();
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("알림 통계 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 활성 알림 조회
     */
    @GetMapping("/alerts/active")
    public ResponseEntity<List<AlertingService.AlertState>> getActiveAlerts() {
        try {
            List<AlertingService.AlertState> activeAlerts = alertingService.getActiveAlerts();
            return ResponseEntity.ok(activeAlerts);
            
        } catch (Exception e) {
            log.error("활성 알림 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 최근 알림 이력 조회
     */
    @GetMapping("/alerts/recent")
    public ResponseEntity<List<AlertingService.AlertEvent>> getRecentAlerts(
            @RequestParam(defaultValue = "50") int count) {
        try {
            List<AlertingService.AlertEvent> recentAlerts = 
                alertingService.getRecentAlerts(count);
            return ResponseEntity.ok(recentAlerts);
            
        } catch (Exception e) {
            log.error("최근 알림 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 수동 알림 전송
     */
    @PostMapping("/alerts/send")
    public ResponseEntity<Map<String, Object>> sendManualAlert(
            @RequestBody ManualAlertRequest request) {
        try {
            alertingService.sendAlert(
                request.getRuleName(),
                request.getMessage(),
                AlertingService.AlertSeverity.valueOf(request.getSeverity().toUpperCase())
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "알림이 전송되었습니다"
            ));
            
        } catch (Exception e) {
            log.error("수동 알림 전송 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * 알림 해결
     */
    @PostMapping("/alerts/resolve/{ruleName}")
    public ResponseEntity<Map<String, Object>> resolveAlert(@PathVariable String ruleName) {
        try {
            alertingService.resolveAlert(ruleName);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "알림이 해결되었습니다"
            ));
            
        } catch (Exception e) {
            log.error("알림 해결 실패: {}", ruleName, e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    // === 퍼블리셔/구독자 통계 ===
    
    /**
     * 퍼블리셔 통계 조회
     */
    @GetMapping("/publisher/stats")
    public ResponseEntity<MessagePublisher.PublishStats> getPublisherStats() {
        try {
            MessagePublisher.PublishStats stats = messagePublisher.getPublishStats();
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("퍼블리셔 통계 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 구독자 통계 조회
     */
    @GetMapping("/subscriber/stats")
    public ResponseEntity<MessageSubscriber.SubscriptionStats> getSubscriberStats() {
        try {
            MessageSubscriber.SubscriptionStats stats = messageSubscriber.getSubscriptionStats();
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("구독자 통계 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 구독 관리 통계
     */
    @GetMapping("/subscriptions/stats")
    public ResponseEntity<RedisSubscriptionConfig.SubscriptionStats> getSubscriptionStats() {
        try {
            RedisSubscriptionConfig.SubscriptionStats stats = subscriptionManager.getSubscriptionStats();
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("구독 통계 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // === 실시간 데이터 ===
    
    /**
     * 실시간 메트릭스 데이터 (SSE)
     */
    @GetMapping(value = "/stream/metrics", produces = "text/event-stream")
    public ResponseEntity<String> streamMetrics() {
        // Server-Sent Events를 위한 실시간 스트리밍
        // 실제 구현에서는 SseEmitter나 WebFlux 사용 권장
        
        try {
            MetricsCollectorService.MetricsSnapshot snapshot = metricsCollector.getCurrentSnapshot();
            
            StringBuilder sseData = new StringBuilder();
            sseData.append("data: ");
            sseData.append(formatAsJson(snapshot));
            sseData.append("\n\n");
            
            return ResponseEntity.ok()
                    .header("Cache-Control", "no-cache")
                    .header("Connection", "keep-alive")
                    .body(sseData.toString());
                    
        } catch (Exception e) {
            log.error("실시간 메트릭스 스트리밍 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // === 설정 관리 ===
    
    /**
     * 모니터링 설정 조회
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getMonitoringConfig() {
        try {
            Map<String, Object> config = new HashMap<>();
            
            // 기본 설정 정보
            config.put("refreshInterval", 30); // 초
            config.put("retentionDays", 7);
            config.put("maxDataPoints", 10000);
            config.put("alertCooldownMinutes", 5);
            
            // 임계치 설정
            config.put("thresholds", Map.of(
                "errorRate", 10.0,
                "latency", 5000.0,
                "memoryUsage", 0.85,
                "connectionFailures", 5.0
            ));
            
            return ResponseEntity.ok(config);
            
        } catch (Exception e) {
            log.error("모니터링 설정 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 임계치 위반 체크
     */
    @GetMapping("/thresholds/violations")
    public ResponseEntity<List<MetricsCollectorService.ThresholdViolation>> getThresholdViolations() {
        try {
            List<MetricsCollectorService.ThresholdViolation> violations = 
                metricsCollector.checkThresholds();
            
            return ResponseEntity.ok(violations);
            
        } catch (Exception e) {
            log.error("임계치 위반 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // === 유틸리티 엔드포인트 ===
    
    /**
     * 모니터링 데이터 요약
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getMonitoringSummary() {
        try {
            Map<String, Object> summary = new HashMap<>();
            
            // 시스템 상태 요약
            PubSubMonitoringService.SystemStatus systemStatus = monitoringService.getSystemStatus();
            summary.put("systemHealth", systemStatus.getHealthStatus());
            summary.put("uptime", systemStatus.getUptime());
            
            // 메트릭스 요약
            MetricsCollectorService.MetricsSnapshot metrics = metricsCollector.getCurrentSnapshot();
            summary.put("totalMessages", getTotalMessages(metrics));
            
            // 알림 요약
            AlertingService.AlertingStats alerts = alertingService.getAlertingStats();
            summary.put("activeAlerts", alerts.getActiveAlerts());
            summary.put("totalAlerts", alerts.getTotalGenerated());
            
            // 성능 요약
            summary.put("performance", Map.of(
                "errorRate", calculateErrorRate(),
                "averageLatency", calculateAverageLatency(),
                "throughput", calculateThroughput()
            ));
            
            return ResponseEntity.ok(summary);
            
        } catch (Exception e) {
            log.error("모니터링 요약 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // === 내부 유틸리티 메서드들 ===
    
    private String formatAsJson(Object object) {
        // 실제로는 Jackson ObjectMapper 사용
        return object.toString();
    }
    
    private long getTotalMessages(MetricsCollectorService.MetricsSnapshot metrics) {
        Object totalMessages = metrics.getCounterValues().get("messages.received.total");
        return totalMessages instanceof Number ? ((Number) totalMessages).longValue() : 0;
    }
    
    private double calculateErrorRate() {
        long totalMessages = metricsCollector.getCounterValue("messages.received.total");
        long failedMessages = metricsCollector.getCounterValue("messages.processed.failed");
        
        return totalMessages > 0 ? (double) failedMessages / totalMessages * 100.0 : 0.0;
    }
    
    private double calculateAverageLatency() {
        LatencyStats latencyStats = metricsCollector.getLatencyStats("message.processing.latency");
        return latencyStats.getAverage();
    }
    
    private double calculateThroughput() {
        // 초당 처리된 메시지 수 계산
        return 0.0; // 구현 필요
    }
    
    // === DTO 클래스들 ===
    
    @lombok.Data
    public static class ManualAlertRequest {
        private String ruleName;
        private String message;
        private String severity;
    }
    
    /**
     * API 응답 래퍼
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;
        private LocalDateTime timestamp;
        
        public static <T> ApiResponse<T> success(T data) {
            return new ApiResponse<>(true, "성공", data, LocalDateTime.now());
        }
        
        public static <T> ApiResponse<T> error(String message) {
            return new ApiResponse<>(false, message, null, LocalDateTime.now());
        }
    }
}