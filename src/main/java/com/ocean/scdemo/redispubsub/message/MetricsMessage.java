package com.ocean.scdemo.redispubsub.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

/**
 * 메트릭스 메시지
 * 
 * 시스템 성능 메트릭스, 비즈니스 메트릭스, KPI 등을 전송
 * 모니터링 및 분석 시스템에서 활용
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MetricsMessage extends BaseMessage {
    
    /**
     * 메트릭 기본 정보
     */
    private String metricType; // PERFORMANCE, BUSINESS, SYSTEM, CUSTOM
    private String metricName;
    private String metricCategory;
    private String unit; // COUNT, PERCENT, MILLISECONDS, BYTES, etc.
    
    /**
     * 수치 데이터
     */
    private Double value;
    private Double minValue;
    private Double maxValue;
    private Double avgValue;
    private Long count;
    private Double sum;
    
    /**
     * 시간 및 주기
     */
    private java.time.LocalDateTime measurementTime;
    private Long timeWindowStart; // 측정 기간 시작 (epoch millis)
    private Long timeWindowEnd;   // 측정 기간 끝 (epoch millis)
    private String aggregationPeriod; // 1m, 5m, 15m, 1h, 1d
    
    /**
     * 소스 정보
     */
    private String source; // 메트릭 소스 (서비스, 모듈 등)
    private String sourceInstance;
    private String hostName;
    private String environment; // DEV, STAGING, PROD
    
    /**
     * 디멘션 데이터 (태그)
     */
    private Map<String, String> dimensions;
    private Map<String, String> labels;
    
    /**
     * 대시보드 및 알림 설정
     */
    private String dashboardName;
    private String chartType; // LINE, BAR, GAUGE, PIE, TABLE
    private Boolean alertEnabled;
    private Double alertThreshold;
    private String alertOperator; // GT, LT, EQ, GTE, LTE
    
    /**
     * 비즈니스 컨텍스트
     */
    private String businessUnit;
    private String productName;
    private String featureName;
    private String userId; // 사용자별 메트릭인 경우
    
    /**
     * 성능 메트릭스
     */
    private Long responseTime; // 응답 시간 (ms)
    private Long requestCount;
    private Long errorCount;
    private Double errorRate;
    private Double throughput; // 처리량 (requests/second)
    private Double cpuUsage;
    private Double memoryUsage;
    private Double diskUsage;
    private Double networkIn;
    private Double networkOut;
    
    /**
     * 비즈니스 메트릭스
     */
    private Long activeUsers;
    private Long newUsers;
    private Long totalRevenue;
    private Long orderCount;
    private Double conversionRate;
    private Double bounceRate;
    private Long sessionCount;
    private Long pageViews;
    
    /**
     * 커스텀 메트릭스
     */
    private Map<String, Object> customMetrics;
    
    /**
     * 메트릭 생성자 편의 메서드
     */
    public static MetricsMessage createPerformanceMetric(String serviceName, 
                                                         String metricName, 
                                                         Double value, 
                                                         String unit) {
        return MetricsMessage.builder()
                .messageType("METRICS")
                .metricType("PERFORMANCE")
                .metricName(metricName)
                .value(value)
                .unit(unit)
                .source(serviceName)
                .measurementTime(java.time.LocalDateTime.now())
                .environment("PROD")
                .build();
    }
    
    public static MetricsMessage createBusinessMetric(String productName, 
                                                      String metricName, 
                                                      Long count, 
                                                      String businessUnit) {
        return MetricsMessage.builder()
                .messageType("METRICS")
                .metricType("BUSINESS")
                .metricName(metricName)
                .count(count)
                .unit("COUNT")
                .productName(productName)
                .businessUnit(businessUnit)
                .measurementTime(java.time.LocalDateTime.now())
                .build();
    }
    
    public static MetricsMessage createSystemMetric(String hostName, 
                                                    Double cpuUsage, 
                                                    Double memoryUsage) {
        return MetricsMessage.builder()
                .messageType("METRICS")
                .metricType("SYSTEM")
                .metricName("system_resources")
                .cpuUsage(cpuUsage)
                .memoryUsage(memoryUsage)
                .unit("PERCENT")
                .hostName(hostName)
                .measurementTime(java.time.LocalDateTime.now())
                .build();
    }
    
    /**
     * 알림 임계치 초과 확인
     */
    public boolean exceedsThreshold() {
        if (alertThreshold == null || value == null) {
            return false;
        }
        
        return switch (alertOperator != null ? alertOperator : "GT") {
            case "GT" -> value > alertThreshold;
            case "GTE" -> value >= alertThreshold;
            case "LT" -> value < alertThreshold;
            case "LTE" -> value <= alertThreshold;
            case "EQ" -> value.equals(alertThreshold);
            default -> false;
        };
    }
    
    /**
     * 대시보드에 표시할 수 있는지 확인
     */
    public boolean isDashboardMetric() {
        return dashboardName != null && chartType != null;
    }
    
    /**
     * 비즈니스 메트릭인지 확인
     */
    public boolean isBusinessMetric() {
        return "BUSINESS".equals(metricType);
    }
    
    /**
     * 성능 메트릭인지 확인
     */
    public boolean isPerformanceMetric() {
        return "PERFORMANCE".equals(metricType);
    }
    
    /**
     * 에러율 계산
     */
    public void calculateErrorRate() {
        if (requestCount != null && errorCount != null && requestCount > 0) {
            this.errorRate = (errorCount.doubleValue() / requestCount.doubleValue()) * 100.0;
        }
    }
    
    /**
     * 처리량 계산
     */
    public void calculateThroughput(Long timeWindowSeconds) {
        if (requestCount != null && timeWindowSeconds != null && timeWindowSeconds > 0) {
            this.throughput = requestCount.doubleValue() / timeWindowSeconds.doubleValue();
        }
    }
    
    /**
     * 디멘션 추가
     */
    public void addDimension(String key, String value) {
        if (dimensions == null) {
            dimensions = new java.util.HashMap<>();
        }
        dimensions.put(key, value);
    }
    
    /**
     * 라벨 추가
     */
    public void addLabel(String key, String value) {
        if (labels == null) {
            labels = new java.util.HashMap<>();
        }
        labels.put(key, value);
    }
    
    /**
     * 커스텀 메트릭 추가
     */
    public void addCustomMetric(String key, Object value) {
        if (customMetrics == null) {
            customMetrics = new java.util.HashMap<>();
        }
        customMetrics.put(key, value);
    }
    
    /**
     * 시간 창 설정
     */
    public void setTimeWindow(Long startMillis, Long endMillis) {
        this.timeWindowStart = startMillis;
        this.timeWindowEnd = endMillis;
    }
    
    @Override
    public BaseMessage copy() {
        return MetricsMessage.builder()
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
                .metricType(this.metricType)
                .metricName(this.metricName)
                .metricCategory(this.metricCategory)
                .unit(this.unit)
                .value(this.value)
                .minValue(this.minValue)
                .maxValue(this.maxValue)
                .avgValue(this.avgValue)
                .count(this.count)
                .sum(this.sum)
                .measurementTime(this.measurementTime)
                .timeWindowStart(this.timeWindowStart)
                .timeWindowEnd(this.timeWindowEnd)
                .aggregationPeriod(this.aggregationPeriod)
                .source(this.source)
                .sourceInstance(this.sourceInstance)
                .hostName(this.hostName)
                .environment(this.environment)
                .dimensions(this.dimensions != null ? new java.util.HashMap<>(this.dimensions) : null)
                .labels(this.labels != null ? new java.util.HashMap<>(this.labels) : null)
                .dashboardName(this.dashboardName)
                .chartType(this.chartType)
                .alertEnabled(this.alertEnabled)
                .alertThreshold(this.alertThreshold)
                .alertOperator(this.alertOperator)
                .businessUnit(this.businessUnit)
                .productName(this.productName)
                .featureName(this.featureName)
                .userId(this.userId)
                .responseTime(this.responseTime)
                .requestCount(this.requestCount)
                .errorCount(this.errorCount)
                .errorRate(this.errorRate)
                .throughput(this.throughput)
                .cpuUsage(this.cpuUsage)
                .memoryUsage(this.memoryUsage)
                .diskUsage(this.diskUsage)
                .networkIn(this.networkIn)
                .networkOut(this.networkOut)
                .activeUsers(this.activeUsers)
                .newUsers(this.newUsers)
                .totalRevenue(this.totalRevenue)
                .orderCount(this.orderCount)
                .conversionRate(this.conversionRate)
                .bounceRate(this.bounceRate)
                .sessionCount(this.sessionCount)
                .pageViews(this.pageViews)
                .customMetrics(this.customMetrics != null ? new java.util.HashMap<>(this.customMetrics) : null)
                .build();
    }
}