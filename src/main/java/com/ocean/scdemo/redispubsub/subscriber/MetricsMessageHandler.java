package com.ocean.scdemo.redispubsub.subscriber;

import com.ocean.scdemo.redispubsub.message.MetricsMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 메트릭스 메시지 처리 핸들러
 * 
 * 기능:
 * - 성능 메트릭 수집 및 분석
 * - 비즈니스 KPI 추적
 * - 실시간 대시보드 데이터 업데이트
 * - 알림 임계치 모니터링
 * - 메트릭 집계 및 계산
 * - 이상치 탐지 및 알림
 */
@Slf4j
@Component
public class MetricsMessageHandler {

    // 처리 통계
    private final AtomicLong totalProcessed = new AtomicLong(0);
    private final AtomicLong performanceMetrics = new AtomicLong(0);
    private final AtomicLong businessMetrics = new AtomicLong(0);
    private final AtomicLong systemMetrics = new AtomicLong(0);
    private final AtomicLong customMetrics = new AtomicLong(0);
    private final AtomicLong alertTriggered = new AtomicLong(0);
    
    // 메트릭 데이터 저장소 (실제로는 시계열 DB 사용)
    private final Map<String, MetricDataPoint> latestMetrics = new ConcurrentHashMap<>();
    
    // 알림 임계치 위반 추적
    private final Map<String, ThresholdViolation> thresholdViolations = new ConcurrentHashMap<>();
    
    public boolean handleMessage(MetricsMessage message) {
        try {
            totalProcessed.incrementAndGet();
            
            // 메트릭 타입별 처리
            boolean processed = switch (message.getMetricType()) {
                case "PERFORMANCE" -> handlePerformanceMetric(message);
                case "BUSINESS" -> handleBusinessMetric(message);
                case "SYSTEM" -> handleSystemMetric(message);
                case "CUSTOM" -> handleCustomMetric(message);
                default -> handleGenericMetric(message);
            };
            
            if (processed) {
                // 메트릭 데이터 저장
                storeMetricData(message);
                
                // 알림 임계치 체크
                checkAlertThresholds(message);
                
                // 대시보드 데이터 업데이트
                updateDashboardData(message);
                
                log.debug("메트릭 처리 완료: 메트릭={}, 값={}, 단위={}, 소스={}", 
                         message.getMetricName(), message.getValue(), message.getUnit(), message.getSource());
            }
            
            return processed;
            
        } catch (Exception e) {
            log.error("메트릭 처리 실패: ID={}", message.getMessageId(), e);
            return false;
        }
    }
    
    /**
     * 성능 메트릭 처리
     */
    private boolean handlePerformanceMetric(MetricsMessage message) {
        performanceMetrics.incrementAndGet();
        
        log.debug("📈 성능 메트릭: 메트릭={}, 값={}, 소스={}", 
                 message.getMetricName(), message.getValue(), message.getSource());
        
        // 성능 메트릭에 따른 세부 처리
        return switch (message.getMetricName()) {
            case "response_time" -> handleResponseTimeMetric(message);
            case "throughput" -> handleThroughputMetric(message);
            case "error_rate" -> handleErrorRateMetric(message);
            case "cpu_usage" -> handleCpuUsageMetric(message);
            case "memory_usage" -> handleMemoryUsageMetric(message);
            default -> handleGenericPerformanceMetric(message);
        };
    }
    
    /**
     * 비즈니스 메트릭 처리
     */
    private boolean handleBusinessMetric(MetricsMessage message) {
        businessMetrics.incrementAndGet();
        
        log.info("💼 비즈니스 메트릭: 메트릭={}, 값={}, 비즈니스유닛={}", 
                message.getMetricName(), message.getValue(), message.getBusinessUnit());
        
        // 비즈니스 메트릭에 따른 세부 처리
        return switch (message.getMetricName()) {
            case "revenue" -> handleRevenueMetric(message);
            case "user_count" -> handleUserCountMetric(message);
            case "conversion_rate" -> handleConversionRateMetric(message);
            case "order_count" -> handleOrderCountMetric(message);
            default -> handleGenericBusinessMetric(message);
        };
    }
    
    /**
     * 시스템 메트릭 처리
     */
    private boolean handleSystemMetric(MetricsMessage message) {
        systemMetrics.incrementAndGet();
        
        log.debug("🔧 시스템 메트릭: 메트릭={}, 값={}, 호스트={}", 
                 message.getMetricName(), message.getValue(), message.getHostName());
        
        // 시스템 메트릭 처리
        return switch (message.getMetricName()) {
            case "system_resources" -> handleSystemResourcesMetric(message);
            case "disk_usage" -> handleDiskUsageMetric(message);
            case "network_io" -> handleNetworkIoMetric(message);
            default -> handleGenericSystemMetric(message);
        };
    }
    
    /**
     * 커스텀 메트릭 처리
     */
    private boolean handleCustomMetric(MetricsMessage message) {
        customMetrics.incrementAndGet();
        
        log.debug("🌐 커스텀 메트릭: 메트릭={}, 값={}", 
                 message.getMetricName(), message.getValue());
        
        // 커스텀 메트릭 처리
        if (message.getCustomMetrics() != null) {
            message.getCustomMetrics().forEach((key, value) -> {
                log.debug("커스텀 데이터: {}={}", key, value);
            });
        }
        
        return true;
    }
    
    /**
     * 일반 메트릭 처리
     */
    private boolean handleGenericMetric(MetricsMessage message) {
        log.debug("📉 일반 메트릭: 타입={}, 메트릭={}, 값={}", 
                 message.getMetricType(), message.getMetricName(), message.getValue());
        
        return true;
    }
    
    // === 성능 메트릭 세부 처리 ===
    
    private boolean handleResponseTimeMetric(MetricsMessage message) {
        Double responseTime = message.getValue();
        if (responseTime != null && responseTime > 1000) { // 1초 초과
            log.warn("🔴 높은 응답시간: {}ms, 소스={}", responseTime, message.getSource());
        }
        return true;
    }
    
    private boolean handleThroughputMetric(MetricsMessage message) {
        log.debug("💨 처리량: {} req/s, 소스={}", message.getValue(), message.getSource());
        return true;
    }
    
    private boolean handleErrorRateMetric(MetricsMessage message) {
        Double errorRate = message.getValue();
        if (errorRate != null && errorRate > 5.0) { // 5% 초과
            log.error("🚨 높은 오류율: {}%, 소스={}", errorRate, message.getSource());
        }
        return true;
    }
    
    private boolean handleCpuUsageMetric(MetricsMessage message) {
        Double cpuUsage = message.getValue();
        if (cpuUsage != null && cpuUsage > 80.0) { // 80% 초과
            log.warn("🟡 높은 CPU 사용량: {}%, 호스트={}", cpuUsage, message.getHostName());
        }
        return true;
    }
    
    private boolean handleMemoryUsageMetric(MetricsMessage message) {
        Double memoryUsage = message.getValue();
        if (memoryUsage != null && memoryUsage > 85.0) { // 85% 초과
            log.warn("🟠 높은 메모리 사용량: {}%, 호스트={}", memoryUsage, message.getHostName());
        }
        return true;
    }
    
    private boolean handleGenericPerformanceMetric(MetricsMessage message) {
        return true;
    }
    
    // === 비즈니스 메트릭 세부 처리 ===
    
    private boolean handleRevenueMetric(MetricsMessage message) {
        log.info("💰 매출: {}, 기간={}", 
                message.getValue(), message.getAggregationPeriod());
        
        // 매출 목표 대비 비교
        checkRevenueTarget(message);
        
        return true;
    }
    
    private boolean handleUserCountMetric(MetricsMessage message) {
        log.info("👥 사용자 수: {} ({})", 
                message.getValue(), message.getMetricName().contains("new") ? "신규" : "전체");
        
        return true;
    }
    
    private boolean handleConversionRateMetric(MetricsMessage message) {
        log.info("🎣 전환율: {}%, 제품={}", 
                message.getValue(), message.getProductName());
        
        return true;
    }
    
    private boolean handleOrderCountMetric(MetricsMessage message) {
        log.info("🛋️ 주문 수: {}, 기간={}", 
                message.getValue(), message.getAggregationPeriod());
        
        return true;
    }
    
    private boolean handleGenericBusinessMetric(MetricsMessage message) {
        return true;
    }
    
    // === 시스템 메트릭 세부 처리 ===
    
    private boolean handleSystemResourcesMetric(MetricsMessage message) {
        log.debug("🔋 시스템 리소스: CPU={}%, 메모리={}%", 
                 message.getCpuUsage(), message.getMemoryUsage());
        
        return true;
    }
    
    private boolean handleDiskUsageMetric(MetricsMessage message) {
        Double diskUsage = message.getValue();
        if (diskUsage != null && diskUsage > 90.0) { // 90% 초과
            log.error("🟥 높은 디스크 사용량: {}%, 호스트={}", diskUsage, message.getHostName());
        }
        
        return true;
    }
    
    private boolean handleNetworkIoMetric(MetricsMessage message) {
        log.debug("🌐 네트워크 I/O: In={} MB/s, Out={} MB/s", 
                 message.getNetworkIn(), message.getNetworkOut());
        
        return true;
    }
    
    private boolean handleGenericSystemMetric(MetricsMessage message) {
        return true;
    }
    
    // === 메트릭 데이터 처리 ===
    
    private void storeMetricData(MetricsMessage message) {
        String metricKey = buildMetricKey(message);
        
        MetricDataPoint dataPoint = MetricDataPoint.builder()
                .metricName(message.getMetricName())
                .value(message.getValue())
                .timestamp(message.getMeasurementTime())
                .source(message.getSource())
                .unit(message.getUnit())
                .dimensions(message.getDimensions())
                .build();
        
        latestMetrics.put(metricKey, dataPoint);
        
        // 실제로는 시계열 DB나 메트릭 저장소에 저장
        // timeSeriesDB.store(dataPoint);
    }
    
    private void checkAlertThresholds(MetricsMessage message) {
        if (message.exceedsThreshold()) {
            alertTriggered.incrementAndGet();
            
            String alertKey = message.getMetricName() + ":" + message.getSource();
            ThresholdViolation violation = thresholdViolations.computeIfAbsent(alertKey, 
                k -> new ThresholdViolation());
            
            violation.incrementCount();
            violation.setLastViolation(LocalDateTime.now());
            
            log.warn("🚨 메트릭 임계치 위반: 메트릭={}, 값={}, 임계치={}, 연산자={}", 
                    message.getMetricName(), message.getValue(), 
                    message.getAlertThreshold(), message.getAlertOperator());
            
            // 알림 전송
            sendAlert(message, violation);
        }
    }
    
    private void updateDashboardData(MetricsMessage message) {
        if (message.isDashboardMetric()) {
            log.debug("📈 대시보드 데이터 업데이트: 대시보드={}, 차트타입={}", 
                     message.getDashboardName(), message.getChartType());
            
            // 실시간 대시보드 데이터 업데이트
            // dashboardService.updateChart(message.getDashboardName(), message);
        }
    }
    
    // === 유틸리티 메서드들 ===
    
    private String buildMetricKey(MetricsMessage message) {
        return String.format("%s:%s:%s", 
                           message.getMetricName(), 
                           message.getSource() != null ? message.getSource() : "unknown",
                           message.getHostName() != null ? message.getHostName() : "unknown");
    }
    
    private void checkRevenueTarget(MetricsMessage message) {
        // 매출 목표 대비 달성률 확인
        Double currentRevenue = message.getValue();
        // Double targetRevenue = getRevenueTarget(); // 목표 값 조회
        
        // if (currentRevenue < targetRevenue * 0.8) { // 80% 미만
        //     log.warn("매출 목표 미달성 위험"); 
        // }
    }
    
    private void sendAlert(MetricsMessage message, ThresholdViolation violation) {
        // 알림 전송 로직 (이메일, 슬랙, SMS 등)
        log.info("알림 전송: 메트릭={}, 위반횟수={}", 
                message.getMetricName(), violation.getCount());
    }
    
    /**
     * 핸들러 통계 조회
     */
    public MetricsHandlerStats getStats() {
        return MetricsHandlerStats.builder()
                .totalProcessed(totalProcessed.get())
                .performanceMetrics(performanceMetrics.get())
                .businessMetrics(businessMetrics.get())
                .systemMetrics(systemMetrics.get())
                .customMetrics(customMetrics.get())
                .alertTriggered(alertTriggered.get())
                .activeMetrics(latestMetrics.size())
                .thresholdViolations(thresholdViolations.size())
                .build();
    }
    
    // === DTO 클래스들 ===
    
    @lombok.Builder
    @lombok.Data
    public static class MetricDataPoint {
        private String metricName;
        private Double value;
        private LocalDateTime timestamp;
        private String source;
        private String unit;
        private Map<String, String> dimensions;
    }
    
    @lombok.Data
    public static class ThresholdViolation {
        private int count = 0;
        private LocalDateTime firstViolation = LocalDateTime.now();
        private LocalDateTime lastViolation;
        
        public void incrementCount() {
            count++;
        }
    }
    
    @lombok.Builder
    @lombok.Data
    public static class MetricsHandlerStats {
        private final long totalProcessed;
        private final long performanceMetrics;
        private final long businessMetrics;
        private final long systemMetrics;
        private final long customMetrics;
        private final long alertTriggered;
        private final int activeMetrics;
        private final int thresholdViolations;
        
        public double getAlertRate() {
            return totalProcessed > 0 ? 
                   ((double) alertTriggered / totalProcessed) * 100.0 : 0.0;
        }
        
        public double getBusinessMetricRate() {
            return totalProcessed > 0 ? 
                   ((double) businessMetrics / totalProcessed) * 100.0 : 0.0;
        }
    }
}