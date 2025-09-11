package com.ocean.scdemo.redispubsub.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;

/**
 * 헬스체크 메시지
 * 
 * 서비스 상태 체크, 또는 dependency 체크를 위한 메시지
 * 로드밸런서, 모니터링 시스템에서 활용
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class HealthCheckMessage extends BaseMessage {
    
    /**
     * 헬스체크 기본 정보
     */
    private String checkType; // SERVICE, DATABASE, CACHE, EXTERNAL_API, DISK, MEMORY
    private String serviceName;
    private String serviceVersion;
    private String instanceId;
    private String hostName;
    private String environment; // DEV, STAGING, PROD
    
    /**
     * 상태 정보
     */
    private String status; // UP, DOWN, DEGRADED, UNKNOWN
    private String previousStatus;
    private java.time.LocalDateTime statusChangedAt;
    private Long uptime; // 가동 시간 (초)
    
    /**
     * 체크 결과
     */
    private Boolean healthy;
    private String healthDetails;
    private List<HealthCheckDetail> checks;
    private Integer healthScore; // 0-100 점수
    
    /**
     * 성능 메트릭스
     */
    private Long responseTime; // 체크 응답 시간 (ms)
    private Double cpuUsage;
    private Double memoryUsage;
    private Double diskUsage;
    private Long freeMemory;
    private Long totalMemory;
    private Long freeDisk;
    private Long totalDisk;
    
    /**
     * 데이터베이스 체크
     */
    private DatabaseHealth databaseHealth;
    
    /**
     * 캐시 체크
     */
    private CacheHealth cacheHealth;
    
    /**
     * 외부 API 체크
     */
    private List<ExternalApiHealth> externalApiHealths;
    
    /**
     * 체크 설정
     */
    private Integer checkIntervalSeconds; // 체크 주기
    private Integer timeoutSeconds; // 타임아웃
    private Integer retryCount; // 재시도 횟수
    
    /**
     * 알림 설정
     */
    private Boolean alertEnabled;
    private String alertLevel; // LOW, MEDIUM, HIGH, CRITICAL
    private List<String> alertRecipients;
    
    /**
     * 추가 정보
     */
    private Map<String, Object> additionalInfo;
    private String[] tags;
    
    /**
     * 헬스체크 생성자 편의 메서드
     */
    public static HealthCheckMessage createServiceHealthCheck(String serviceName, 
                                                             String status, 
                                                             Long responseTime) {
        return HealthCheckMessage.builder()
                .messageType("HEALTH_CHECK")
                .checkType("SERVICE")
                .serviceName(serviceName)
                .status(status)
                .healthy("UP".equals(status))
                .responseTime(responseTime)
                .build();
    }
    
    public static HealthCheckMessage createDatabaseHealthCheck(String serviceName, 
                                                              Boolean connected, 
                                                              Long connectionTime) {
        DatabaseHealth dbHealth = DatabaseHealth.builder()
                .connected(connected)
                .connectionTime(connectionTime)
                .activeConnections(10)
                .maxConnections(100)
                .build();
                
        return HealthCheckMessage.builder()
                .messageType("HEALTH_CHECK")
                .checkType("DATABASE")
                .serviceName(serviceName)
                .status(connected ? "UP" : "DOWN")
                .healthy(connected)
                .databaseHealth(dbHealth)
                .build();
    }
    
    public static HealthCheckMessage createSystemHealthCheck(String hostName,
                                                           Double cpuUsage,
                                                           Double memoryUsage,
                                                           Double diskUsage) {
        boolean healthy = cpuUsage < 80.0 && memoryUsage < 80.0 && diskUsage < 90.0;
        String status = healthy ? "UP" : "DEGRADED";
        
        return HealthCheckMessage.builder()
                .messageType("HEALTH_CHECK")
                .checkType("SYSTEM")
                .hostName(hostName)
                .status(status)
                .healthy(healthy)
                .cpuUsage(cpuUsage)
                .memoryUsage(memoryUsage)
                .diskUsage(diskUsage)
                .healthScore(healthy ? 100 : 50)
                .build();
    }
    
    /**
     * 서비스가 건강한지 확인
     */
    public boolean isHealthy() {
        return healthy != null && healthy;
    }
    
    /**
     * 서비스가 정상 작동 중인지 확인
     */
    public boolean isUp() {
        return "UP".equals(status);
    }
    
    /**
     * 서비스가 중단되었는지 확인
     */
    public boolean isDown() {
        return "DOWN".equals(status);
    }
    
    /**
     * 서비스 성능이 저하되었는지 확인
     */
    public boolean isDegraded() {
        return "DEGRADED".equals(status);
    }
    
    /**
     * 알림이 필요한지 확인
     */
    public boolean needsAlert() {
        return alertEnabled != null && alertEnabled && 
               (isDown() || isDegraded() || (healthScore != null && healthScore < 70));
    }
    
    /**
     * 상태 변경 처리
     */
    public void updateStatus(String newStatus) {
        if (!newStatus.equals(this.status)) {
            this.previousStatus = this.status;
            this.status = newStatus;
            this.statusChangedAt = java.time.LocalDateTime.now();
            this.healthy = "UP".equals(newStatus);
        }
    }
    
    /**
     * 체크 결과 추가
     */
    public void addCheck(String checkName, Boolean passed, String details) {
        if (checks == null) {
            checks = new java.util.ArrayList<>();
        }
        checks.add(HealthCheckDetail.builder()
                .checkName(checkName)
                .passed(passed)
                .details(details)
                .checkTime(java.time.LocalDateTime.now())
                .build());
    }
    
    /**
     * 추가 정보 추가
     */
    public void addAdditionalInfo(String key, Object value) {
        if (additionalInfo == null) {
            additionalInfo = new java.util.HashMap<>();
        }
        additionalInfo.put(key, value);
    }
    
    /**
     * 전체 헬스 점수 계산
     */
    public void calculateHealthScore() {
        if (checks == null || checks.isEmpty()) {
            return;
        }
        
        long passedChecks = checks.stream()
                .mapToLong(check -> check.getPassed() ? 1 : 0)
                .sum();
        
        this.healthScore = (int) ((passedChecks * 100.0) / checks.size());
    }
    
    @Override
    public BaseMessage copy() {
        return HealthCheckMessage.builder()
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
                .checkType(this.checkType)
                .serviceName(this.serviceName)
                .serviceVersion(this.serviceVersion)
                .instanceId(this.instanceId)
                .hostName(this.hostName)
                .environment(this.environment)
                .status(this.status)
                .previousStatus(this.previousStatus)
                .statusChangedAt(this.statusChangedAt)
                .uptime(this.uptime)
                .healthy(this.healthy)
                .healthDetails(this.healthDetails)
                .checks(this.checks != null ? new java.util.ArrayList<>(this.checks) : null)
                .healthScore(this.healthScore)
                .responseTime(this.responseTime)
                .cpuUsage(this.cpuUsage)
                .memoryUsage(this.memoryUsage)
                .diskUsage(this.diskUsage)
                .freeMemory(this.freeMemory)
                .totalMemory(this.totalMemory)
                .freeDisk(this.freeDisk)
                .totalDisk(this.totalDisk)
                .databaseHealth(this.databaseHealth)
                .cacheHealth(this.cacheHealth)
                .externalApiHealths(this.externalApiHealths != null ? new java.util.ArrayList<>(this.externalApiHealths) : null)
                .checkIntervalSeconds(this.checkIntervalSeconds)
                .timeoutSeconds(this.timeoutSeconds)
                .retryCount(this.retryCount)
                .alertEnabled(this.alertEnabled)
                .alertLevel(this.alertLevel)
                .alertRecipients(this.alertRecipients != null ? new java.util.ArrayList<>(this.alertRecipients) : null)
                .additionalInfo(this.additionalInfo != null ? new java.util.HashMap<>(this.additionalInfo) : null)
                .tags(this.tags != null ? this.tags.clone() : null)
                .build();
    }
    
    /**
     * 개별 헬스체크 상세 정보
     */
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HealthCheckDetail {
        private String checkName;
        private Boolean passed;
        private String details;
        private Long responseTime;
        private String errorMessage;
        private java.time.LocalDateTime checkTime;
    }
    
    /**
     * 데이터베이스 체크 정보
     */
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatabaseHealth {
        private Boolean connected;
        private Long connectionTime;
        private Integer activeConnections;
        private Integer maxConnections;
        private Long queryResponseTime;
        private String version;
        private Map<String, Object> stats;
    }
    
    /**
     * 캐시 체크 정보
     */
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CacheHealth {
        private Boolean connected;
        private Long connectionTime;
        private Integer activeConnections;
        private Long hitRate;
        private Long missRate;
        private Long keyCount;
        private Long usedMemory;
        private Long maxMemory;
    }
    
    /**
     * 외부 API 체크 정보
     */
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExternalApiHealth {
        private String apiName;
        private String endpoint;
        private Boolean available;
        private Long responseTime;
        private Integer statusCode;
        private String errorMessage;
        private java.time.LocalDateTime lastChecked;
    }
}