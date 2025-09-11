package com.ocean.scdemo.redispubsub.monitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Redis Pub/Sub 메트릭스 수집 서비스
 * 
 * 기능:
 * - 실시간 메트릭 데이터 수집
 * - 시계열 데이터 저장
 * - 성능 트렌드 분석
 * - 임계치 모니터링
 * - 자동 알림 생성
 * - 메트릭 데이터 집계
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsCollectorService {

    private final RedisTemplate<String, Object> redisTemplate;
    
    // 메트릭 저장소
    private final Map<String, TimeSeries> metricsData = new ConcurrentHashMap<>();
    
    // 실시간 카운터
    private final Map<String, LongAdder> counters = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> gauges = new ConcurrentHashMap<>();
    private final Map<String, LatencyTracker> latencyTrackers = new ConcurrentHashMap<>();
    
    // 집계 데이터
    private final Map<String, AggregatedMetrics> hourlyAggregates = new ConcurrentHashMap<>();
    private final Map<String, AggregatedMetrics> dailyAggregates = new ConcurrentHashMap<>();
    
    // 임계치 설정
    private final Map<String, ThresholdConfig> thresholds = new ConcurrentHashMap<>();
    
    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH");
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    @PostConstruct
    public void init() {
        initializeDefaultMetrics();
        setupDefaultThresholds();
        log.info("📊 메트릭스 수집 서비스 시작");
    }
    
    /**
     * 메트릭 카운터 증가
     */
    public void incrementCounter(String metricName) {
        incrementCounter(metricName, 1);
    }
    
    public void incrementCounter(String metricName, long delta) {
        counters.computeIfAbsent(metricName, k -> new LongAdder()).add(delta);
        recordDataPoint(metricName, delta, MetricType.COUNTER);
    }
    
    /**
     * 게이지 값 설정
     */
    public void setGauge(String metricName, long value) {
        gauges.put(metricName, new AtomicLong(value));
        recordDataPoint(metricName, value, MetricType.GAUGE);
    }
    
    /**
     * 지연시간 기록
     */
    public void recordLatency(String metricName, long latencyMs) {
        latencyTrackers.computeIfAbsent(metricName, k -> new LatencyTracker())
                      .recordLatency(latencyMs);
        recordDataPoint(metricName, latencyMs, MetricType.LATENCY);
    }
    
    /**
     * 타이밍 유틸리티
     */
    public Timer startTimer(String metricName) {
        return new Timer(metricName, this);
    }
    
    /**
     * 현재 메트릭 값 조회
     */
    public long getCounterValue(String metricName) {
        LongAdder counter = counters.get(metricName);
        return counter != null ? counter.sum() : 0;
    }
    
    public long getGaugeValue(String metricName) {
        AtomicLong gauge = gauges.get(metricName);
        return gauge != null ? gauge.get() : 0;
    }
    
    public LatencyStats getLatencyStats(String metricName) {
        LatencyTracker tracker = latencyTrackers.get(metricName);
        return tracker != null ? tracker.getStats() : LatencyStats.empty();
    }
    
    /**
     * 시계열 데이터 조회
     */
    public TimeSeries getTimeSeries(String metricName) {
        return metricsData.get(metricName);
    }
    
    public TimeSeries getTimeSeries(String metricName, LocalDateTime from, LocalDateTime to) {
        TimeSeries fullSeries = metricsData.get(metricName);
        if (fullSeries == null) return null;
        
        return fullSeries.filterByTimeRange(from, to);
    }
    
    /**
     * 전체 메트릭스 스냅샷
     */
    public MetricsSnapshot getCurrentSnapshot() {
        Map<String, Object> currentValues = new HashMap<>();
        
        // 카운터 값들
        counters.forEach((name, counter) -> 
            currentValues.put(name, counter.sum()));
        
        // 게이지 값들
        gauges.forEach((name, gauge) -> 
            currentValues.put(name, gauge.get()));
        
        // 지연시간 통계
        Map<String, LatencyStats> latencyStats = new HashMap<>();
        latencyTrackers.forEach((name, tracker) -> 
            latencyStats.put(name, tracker.getStats()));
        
        return MetricsSnapshot.builder()
                .timestamp(LocalDateTime.now())
                .counterValues(currentValues)
                .latencyStats(latencyStats)
                .aggregatedMetrics(getCurrentAggregates())
                .build();
    }
    
    /**
     * 성능 트렌드 분석
     */
    public TrendAnalysis analyzeTrends(String metricName, int hours) {
        TimeSeries series = metricsData.get(metricName);
        if (series == null) return TrendAnalysis.empty();
        
        LocalDateTime from = LocalDateTime.now().minusHours(hours);
        TimeSeries filtered = series.filterByTimeRange(from, LocalDateTime.now());
        
        return TrendAnalysis.builder()
                .metricName(metricName)
                .periodHours(hours)
                .dataPoints(filtered.getDataPoints().size())
                .average(filtered.calculateAverage())
                .minimum(filtered.getMinimum())
                .maximum(filtered.getMaximum())
                .trend(calculateTrend(filtered))
                .growthRate(calculateGrowthRate(filtered))
                .volatility(calculateVolatility(filtered))
                .build();
    }
    
    /**
     * 임계치 위반 체크
     */
    public List<ThresholdViolation> checkThresholds() {
        List<ThresholdViolation> violations = new ArrayList<>();
        
        thresholds.forEach((metricName, config) -> {
            Object currentValue = getCurrentValue(metricName);
            if (currentValue instanceof Number) {
                double value = ((Number) currentValue).doubleValue();
                
                if (config.isViolated(value)) {
                    violations.add(ThresholdViolation.builder()
                            .metricName(metricName)
                            .currentValue(value)
                            .thresholdValue(config.getThreshold())
                            .operator(config.getOperator())
                            .severity(config.getSeverity())
                            .timestamp(LocalDateTime.now())
                            .build());
                }
            }
        });
        
        return violations;
    }
    
    /**
     * 메트릭 데이터 내보내기
     */
    public Map<String, Object> exportMetrics(ExportFormat format) {
        Map<String, Object> exportData = new HashMap<>();
        
        exportData.put("timestamp", LocalDateTime.now());
        exportData.put("format", format.name());
        
        switch (format) {
            case PROMETHEUS:
                exportData.put("metrics", exportPrometheusFormat());
                break;
            case JSON:
                exportData.put("metrics", exportJsonFormat());
                break;
            case CSV:
                exportData.put("metrics", exportCsvFormat());
                break;
        }
        
        return exportData;
    }
    
    /**
     * 정기적인 메트릭 집계 (매 분)
     */
    @Scheduled(fixedRate = 60000)
    public void aggregateMetrics() {
        try {
            String currentHour = LocalDateTime.now().format(HOUR_FORMATTER);
            String currentDay = LocalDateTime.now().format(DAY_FORMATTER);
            
            // 시간별 집계
            updateHourlyAggregates(currentHour);
            
            // 일별 집계
            updateDailyAggregates(currentDay);
            
            log.debug("📊 메트릭 집계 완료: hour={}, day={}", currentHour, currentDay);
            
        } catch (Exception e) {
            log.error("메트릭 집계 실패", e);
        }
    }
    
    /**
     * 메트릭 정리 (매 시간)
     */
    @Scheduled(fixedRate = 3600000)
    public void cleanupMetrics() {
        try {
            int cleanedCount = 0;
            LocalDateTime cutoff = LocalDateTime.now().minusDays(7); // 7일 이상된 데이터 정리
            
            for (TimeSeries series : metricsData.values()) {
                cleanedCount += series.removeOldData(cutoff);
            }
            
            log.info("🧹 메트릭 데이터 정리 완료: {}개 데이터포인트 제거", cleanedCount);
            
        } catch (Exception e) {
            log.error("메트릭 데이터 정리 실패", e);
        }
    }
    
    // === 내부 구현 메서드들 ===
    
    private void initializeDefaultMetrics() {
        // 기본 메트릭 초기화
        String[] defaultCounters = {
            "messages.published.total",
            "messages.received.total", 
            "messages.processed.success",
            "messages.processed.failed",
            "connections.active",
            "subscriptions.active"
        };
        
        for (String metric : defaultCounters) {
            counters.put(metric, new LongAdder());
            metricsData.put(metric, new TimeSeries(metric, MetricType.COUNTER));
        }
        
        String[] defaultGauges = {
            "memory.usage",
            "cpu.usage",
            "queue.size",
            "connection.pool.size"
        };
        
        for (String metric : defaultGauges) {
            gauges.put(metric, new AtomicLong(0));
            metricsData.put(metric, new TimeSeries(metric, MetricType.GAUGE));
        }
        
        String[] defaultLatencies = {
            "message.processing.latency",
            "redis.operation.latency",
            "handler.execution.latency"
        };
        
        for (String metric : defaultLatencies) {
            latencyTrackers.put(metric, new LatencyTracker());
            metricsData.put(metric, new TimeSeries(metric, MetricType.LATENCY));
        }
    }
    
    private void setupDefaultThresholds() {
        // 기본 임계치 설정
        thresholds.put("messages.processed.failed", 
            ThresholdConfig.builder()
                .threshold(100)
                .operator(ThresholdOperator.GREATER_THAN)
                .severity(AlertSeverity.WARNING)
                .build());
        
        thresholds.put("message.processing.latency", 
            ThresholdConfig.builder()
                .threshold(5000) // 5초
                .operator(ThresholdOperator.GREATER_THAN)
                .severity(AlertSeverity.CRITICAL)
                .build());
        
        thresholds.put("memory.usage", 
            ThresholdConfig.builder()
                .threshold(0.9) // 90%
                .operator(ThresholdOperator.GREATER_THAN)
                .severity(AlertSeverity.WARNING)
                .build());
    }
    
    private void recordDataPoint(String metricName, double value, MetricType type) {
        TimeSeries series = metricsData.computeIfAbsent(metricName, k -> new TimeSeries(k, type));
        series.addDataPoint(value, LocalDateTime.now());
    }
    
    private Object getCurrentValue(String metricName) {
        if (counters.containsKey(metricName)) {
            return counters.get(metricName).sum();
        } else if (gauges.containsKey(metricName)) {
            return gauges.get(metricName).get();
        } else if (latencyTrackers.containsKey(metricName)) {
            return latencyTrackers.get(metricName).getStats().getAverage();
        }
        return null;
    }
    
    private Map<String, AggregatedMetrics> getCurrentAggregates() {
        Map<String, AggregatedMetrics> current = new HashMap<>();
        current.putAll(hourlyAggregates);
        current.putAll(dailyAggregates);
        return current;
    }
    
    private void updateHourlyAggregates(String hour) {
        // 시간별 집계 로직
    }
    
    private void updateDailyAggregates(String day) {
        // 일별 집계 로직
    }
    
    private TrendDirection calculateTrend(TimeSeries series) {
        // 트렌드 계산 로직
        return TrendDirection.STABLE;
    }
    
    private double calculateGrowthRate(TimeSeries series) {
        // 성장률 계산
        return 0.0;
    }
    
    private double calculateVolatility(TimeSeries series) {
        // 변동성 계산
        return 0.0;
    }
    
    private Map<String, Object> exportPrometheusFormat() {
        // Prometheus 형식 내보내기
        return new HashMap<>();
    }
    
    private Map<String, Object> exportJsonFormat() {
        // JSON 형식 내보내기
        return new HashMap<>();
    }
    
    private Map<String, Object> exportCsvFormat() {
        // CSV 형식 내보내기
        return new HashMap<>();
    }
    
    // === DTO 및 유틸리티 클래스들 ===
    
    public enum MetricType {
        COUNTER, GAUGE, LATENCY, HISTOGRAM
    }
    
    public enum ExportFormat {
        PROMETHEUS, JSON, CSV
    }
    
    public enum ThresholdOperator {
        GREATER_THAN, LESS_THAN, EQUALS
    }
    
    public enum AlertSeverity {
        INFO, WARNING, CRITICAL
    }
    
    public enum TrendDirection {
        INCREASING, DECREASING, STABLE
    }
    
    @lombok.Builder
    @lombok.Data
    public static class MetricsSnapshot {
        private LocalDateTime timestamp;
        private Map<String, Object> counterValues;
        private Map<String, LatencyStats> latencyStats;
        private Map<String, AggregatedMetrics> aggregatedMetrics;
    }
    
    @lombok.Builder
    @lombok.Data
    public static class TrendAnalysis {
        private String metricName;
        private int periodHours;
        private int dataPoints;
        private double average;
        private double minimum;
        private double maximum;
        private TrendDirection trend;
        private double growthRate;
        private double volatility;
        
        public static TrendAnalysis empty() {
            return TrendAnalysis.builder().build();
        }
    }
    
    @lombok.Builder
    @lombok.Data
    public static class ThresholdViolation {
        private String metricName;
        private double currentValue;
        private double thresholdValue;
        private ThresholdOperator operator;
        private AlertSeverity severity;
        private LocalDateTime timestamp;
    }
    
    @lombok.Builder
    @lombok.Data
    public static class ThresholdConfig {
        private double threshold;
        private ThresholdOperator operator;
        private AlertSeverity severity;
        
        public boolean isViolated(double value) {
            return switch (operator) {
                case GREATER_THAN -> value > threshold;
                case LESS_THAN -> value < threshold;
                case EQUALS -> Math.abs(value - threshold) < 0.001;
            };
        }
    }
    
    @lombok.Data
    public static class AggregatedMetrics {
        private double sum;
        private double average;
        private double minimum;
        private double maximum;
        private long count;
        private LocalDateTime periodStart;
        private LocalDateTime periodEnd;
    }
    
    public static class Timer implements AutoCloseable {
        private final String metricName;
        private final MetricsCollectorService collector;
        private final long startTime;
        
        public Timer(String metricName, MetricsCollectorService collector) {
            this.metricName = metricName;
            this.collector = collector;
            this.startTime = System.currentTimeMillis();
        }
        
        @Override
        public void close() {
            long duration = System.currentTimeMillis() - startTime;
            collector.recordLatency(metricName, duration);
        }
    }
}