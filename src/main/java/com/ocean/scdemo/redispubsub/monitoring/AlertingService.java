package com.ocean.scdemo.redispubsub.monitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Redis Pub/Sub 시스템 알림 서비스
 * 
 * 기능:
 * - 임계치 위반 감지 및 알림
 * - 다양한 알림 채널 지원 (이메일, 슬랙, SMS, 웹훅)
 * - 알림 중복 방지 및 집계
 * - 에스컬레이션 정책
 * - 알림 이력 관리
 * - 자동 복구 감지
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertingService {

    private final MetricsCollectorService metricsCollector;
    
    // 알림 설정
    private final Map<String, AlertRule> alertRules = new ConcurrentHashMap<>();
    private final Map<String, AlertState> activeAlerts = new ConcurrentHashMap<>();
    private final Queue<AlertEvent> alertHistory = new ConcurrentLinkedQueue<>();
    
    // 알림 채널
    private final List<AlertChannel> alertChannels = new ArrayList<>();
    
    // 알림 집계
    private final Map<String, AlertAggregator> aggregators = new ConcurrentHashMap<>();
    
    // 에스컬레이션
    private final Map<String, EscalationPolicy> escalationPolicies = new ConcurrentHashMap<>();
    
    // 알림 통계
    private long totalAlertsGenerated = 0;
    private long totalAlertsSent = 0;
    private long totalAlertsResolved = 0;
    
    private static final int ALERT_HISTORY_MAX_SIZE = 1000;
    private static final DateTimeFormatter ALERT_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @PostConstruct
    public void init() {
        setupDefaultAlertRules();
        setupDefaultChannels();
        setupDefaultEscalationPolicies();
        
        log.info("🚨 알림 서비스 시작 - 규칙 {}개, 채널 {}개", 
                alertRules.size(), alertChannels.size());
    }
    
    /**
     * 알림 규칙 추가
     */
    public void addAlertRule(String name, AlertRule rule) {
        alertRules.put(name, rule);
        log.info("알림 규칙 추가: {}", name);
    }
    
    /**
     * 알림 채널 추가
     */
    public void addAlertChannel(AlertChannel channel) {
        alertChannels.add(channel);
        log.info("알림 채널 추가: {}", channel.getName());
    }
    
    /**
     * 즉시 알림 전송
     */
    @Async
    public void sendAlert(String ruleName, String message, AlertSeverity severity) {
        sendAlert(Alert.builder()
                .ruleName(ruleName)
                .message(message)
                .severity(severity)
                .timestamp(LocalDateTime.now())
                .source("manual")
                .build());
    }
    
    /**
     * 알림 전송
     */
    @Async
    public void sendAlert(Alert alert) {
        try {
            totalAlertsGenerated++;
            
            // 알림 중복 확인
            if (isDuplicateAlert(alert)) {
                log.debug("중복 알림 무시: {}", alert.getRuleName());
                return;
            }
            
            // 활성 상태로 등록
            registerActiveAlert(alert);
            
            // 알림 집계 처리
            processAggregation(alert);
            
            // 에스컬레이션 정책 적용
            applyEscalationPolicy(alert);
            
            // 각 채널로 전송
            for (AlertChannel channel : alertChannels) {
                if (channel.shouldSendAlert(alert)) {
                    channel.sendAlert(alert);
                    totalAlertsSent++;
                }
            }
            
            // 이력에 추가
            addToHistory(AlertEvent.builder()
                    .alert(alert)
                    .action(AlertAction.SENT)
                    .timestamp(LocalDateTime.now())
                    .build());
            
            log.info("🚨 알림 전송: {} - {} [{}]", 
                    alert.getRuleName(), alert.getMessage(), alert.getSeverity());
                    
        } catch (Exception e) {
            log.error("알림 전송 실패", e);
        }
    }
    
    /**
     * 알림 해결
     */
    public void resolveAlert(String ruleName) {
        AlertState alertState = activeAlerts.remove(ruleName);
        if (alertState != null) {
            totalAlertsResolved++;
            
            Alert resolvedAlert = Alert.builder()
                    .ruleName(ruleName)
                    .message("문제가 해결되었습니다")
                    .severity(AlertSeverity.INFO)
                    .timestamp(LocalDateTime.now())
                    .source("system")
                    .resolved(true)
                    .build();
            
            // 해결 알림 전송
            for (AlertChannel channel : alertChannels) {
                if (channel.shouldSendAlert(resolvedAlert)) {
                    channel.sendAlert(resolvedAlert);
                }
            }
            
            // 이력에 추가
            addToHistory(AlertEvent.builder()
                    .alert(resolvedAlert)
                    .action(AlertAction.RESOLVED)
                    .timestamp(LocalDateTime.now())
                    .build());
            
            log.info("✅ 알림 해결: {}", ruleName);
        }
    }
    
    /**
     * 정기적인 임계치 체크 (매 30초)
     */
    @Scheduled(fixedRate = 30000)
    public void checkThresholds() {
        try {
            List<MetricsCollectorService.ThresholdViolation> violations = 
                metricsCollector.checkThresholds();
            
            for (MetricsCollectorService.ThresholdViolation violation : violations) {
                processThresholdViolation(violation);
            }
            
            // 자동 복구 감지
            checkAutoRecovery();
            
        } catch (Exception e) {
            log.error("임계치 체크 실패", e);
        }
    }
    
    /**
     * 알림 통계 조회
     */
    public AlertingStats getAlertingStats() {
        return AlertingStats.builder()
                .totalGenerated(totalAlertsGenerated)
                .totalSent(totalAlertsSent)
                .totalResolved(totalAlertsResolved)
                .activeAlerts(activeAlerts.size())
                .alertRules(alertRules.size())
                .alertChannels(alertChannels.size())
                .recentAlerts(getRecentAlerts(10))
                .build();
    }
    
    /**
     * 최근 알림 조회
     */
    public List<AlertEvent> getRecentAlerts(int count) {
        return alertHistory.stream()
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(count)
                .toList();
    }
    
    /**
     * 활성 알림 조회
     */
    public List<AlertState> getActiveAlerts() {
        return new ArrayList<>(activeAlerts.values());
    }
    
    // === 내부 구현 메서드들 ===
    
    private void setupDefaultAlertRules() {
        // 높은 오류율 알림
        addAlertRule("high_error_rate", AlertRule.builder()
                .name("high_error_rate")
                .description("높은 오류율 감지")
                .metricName("messages.processed.failed")
                .threshold(10.0)
                .operator(ThresholdOperator.GREATER_THAN)
                .severity(AlertSeverity.CRITICAL)
                .cooldownMinutes(5)
                .enabled(true)
                .build());
        
        // 높은 지연시간 알림
        addAlertRule("high_latency", AlertRule.builder()
                .name("high_latency")
                .description("높은 처리 지연시간")
                .metricName("message.processing.latency")
                .threshold(5000.0) // 5초
                .operator(ThresholdOperator.GREATER_THAN)
                .severity(AlertSeverity.WARNING)
                .cooldownMinutes(2)
                .enabled(true)
                .build());
        
        // 메모리 사용량 알림
        addAlertRule("high_memory_usage", AlertRule.builder()
                .name("high_memory_usage")
                .description("높은 메모리 사용량")
                .metricName("memory.usage")
                .threshold(0.85) // 85%
                .operator(ThresholdOperator.GREATER_THAN)
                .severity(AlertSeverity.WARNING)
                .cooldownMinutes(10)
                .enabled(true)
                .build());
        
        // 연결 실패 알림
        addAlertRule("connection_failure", AlertRule.builder()
                .name("connection_failure")
                .description("Redis 연결 실패")
                .metricName("connections.failed")
                .threshold(5.0)
                .operator(ThresholdOperator.GREATER_THAN)
                .severity(AlertSeverity.CRITICAL)
                .cooldownMinutes(1)
                .enabled(true)
                .build());
    }
    
    private void setupDefaultChannels() {
        // 로그 채널
        alertChannels.add(new LogAlertChannel());
        
        // 콘솔 채널
        alertChannels.add(new ConsoleAlertChannel());
        
        // 실제 환경에서는 이메일, 슬랙 등 추가
        // alertChannels.add(new EmailAlertChannel(...));
        // alertChannels.add(new SlackAlertChannel(...));
    }
    
    private void setupDefaultEscalationPolicies() {
        // 기본 에스컬레이션 정책
        escalationPolicies.put("default", EscalationPolicy.builder()
                .name("default")
                .levels(Arrays.asList(
                    EscalationLevel.builder()
                        .delayMinutes(0)
                        .severity(AlertSeverity.INFO)
                        .channels(List.of("log"))
                        .build(),
                    EscalationLevel.builder()
                        .delayMinutes(5)
                        .severity(AlertSeverity.WARNING)
                        .channels(List.of("log", "console"))
                        .build(),
                    EscalationLevel.builder()
                        .delayMinutes(15)
                        .severity(AlertSeverity.CRITICAL)
                        .channels(List.of("log", "console", "email"))
                        .build()
                ))
                .build());
    }
    
    private boolean isDuplicateAlert(Alert alert) {
        AlertState existingState = activeAlerts.get(alert.getRuleName());
        if (existingState == null) {
            return false;
        }
        
        // 쿨다운 기간 확인
        AlertRule rule = alertRules.get(alert.getRuleName());
        if (rule != null) {
            LocalDateTime cooldownEnd = existingState.getLastSentTime()
                    .plusMinutes(rule.getCooldownMinutes());
            return LocalDateTime.now().isBefore(cooldownEnd);
        }
        
        return false;
    }
    
    private void registerActiveAlert(Alert alert) {
        AlertState state = AlertState.builder()
                .ruleName(alert.getRuleName())
                .firstTriggeredTime(LocalDateTime.now())
                .lastTriggeredTime(LocalDateTime.now())
                .lastSentTime(LocalDateTime.now())
                .count(1)
                .severity(alert.getSeverity())
                .resolved(false)
                .build();
        
        AlertState existing = activeAlerts.get(alert.getRuleName());
        if (existing != null) {
            state.setFirstTriggeredTime(existing.getFirstTriggeredTime());
            state.setCount(existing.getCount() + 1);
        }
        
        activeAlerts.put(alert.getRuleName(), state);
    }
    
    private void processAggregation(Alert alert) {
        // 알림 집계 로직 (예: 1분간 같은 알림 10개 이상 시 집계)
        AlertAggregator aggregator = aggregators.computeIfAbsent(
                alert.getRuleName(), k -> new AlertAggregator());
        
        aggregator.addAlert(alert);
    }
    
    private void applyEscalationPolicy(Alert alert) {
        EscalationPolicy policy = escalationPolicies.get("default");
        if (policy != null) {
            // 에스컬레이션 로직 적용
        }
    }
    
    private void processThresholdViolation(MetricsCollectorService.ThresholdViolation violation) {
        AlertRule rule = findRuleForMetric(violation.getMetricName());
        if (rule != null && rule.isEnabled()) {
            
            Alert alert = Alert.builder()
                    .ruleName(rule.getName())
                    .message(String.format("%s: 현재값=%.2f, 임계치=%.2f", 
                            rule.getDescription(), 
                            violation.getCurrentValue(),
                            violation.getThresholdValue()))
                    .severity(rule.getSeverity())
                    .timestamp(violation.getTimestamp())
                    .source("threshold_monitor")
                    .metricName(violation.getMetricName())
                    .currentValue(violation.getCurrentValue())
                    .thresholdValue(violation.getThresholdValue())
                    .build();
            
            sendAlert(alert);
        }
    }
    
    private AlertRule findRuleForMetric(String metricName) {
        return alertRules.values().stream()
                .filter(rule -> rule.getMetricName().equals(metricName))
                .findFirst()
                .orElse(null);
    }
    
    private void checkAutoRecovery() {
        // 활성 알림 중 조건이 개선된 것들을 자동 해결
        List<String> resolvedRules = new ArrayList<>();
        
        for (Map.Entry<String, AlertState> entry : activeAlerts.entrySet()) {
            String ruleName = entry.getKey();
            AlertRule rule = alertRules.get(ruleName);
            
            if (rule != null && isConditionResolved(rule)) {
                resolvedRules.add(ruleName);
            }
        }
        
        resolvedRules.forEach(this::resolveAlert);
    }
    
    private boolean isConditionResolved(AlertRule rule) {
        // 메트릭 값을 확인하여 임계치를 벗어났는지 확인
        Object currentValue = getCurrentMetricValue(rule.getMetricName());
        if (currentValue instanceof Number) {
            double value = ((Number) currentValue).doubleValue();
            
            return switch (rule.getOperator()) {
                case GREATER_THAN -> value <= rule.getThreshold() * 0.9; // 10% 여유
                case LESS_THAN -> value >= rule.getThreshold() * 1.1;
                case EQUALS -> Math.abs(value - rule.getThreshold()) > rule.getThreshold() * 0.1;
            };
        }
        
        return false;
    }
    
    private Object getCurrentMetricValue(String metricName) {
        return metricsCollector.getCounterValue(metricName);
    }
    
    private void addToHistory(AlertEvent event) {
        alertHistory.offer(event);
        
        // 이력 크기 제한
        while (alertHistory.size() > ALERT_HISTORY_MAX_SIZE) {
            alertHistory.poll();
        }
    }
    
    // === 기본 알림 채널 구현 ===
    
    private static class LogAlertChannel implements AlertChannel {
        @Override
        public String getName() {
            return "log";
        }
        
        @Override
        public boolean shouldSendAlert(Alert alert) {
            return true; // 모든 알림을 로그로 기록
        }
        
        @Override
        public void sendAlert(Alert alert) {
            String message = String.format("[ALERT] %s | %s | %s | %s",
                    alert.getSeverity(),
                    alert.getRuleName(),
                    alert.getMessage(),
                    alert.getTimestamp().format(ALERT_TIME_FORMAT));
            
            switch (alert.getSeverity()) {
                case CRITICAL -> log.error(message);
                case WARNING -> log.warn(message);
                default -> log.info(message);
            }
        }
    }
    
    private static class ConsoleAlertChannel implements AlertChannel {
        @Override
        public String getName() {
            return "console";
        }
        
        @Override
        public boolean shouldSendAlert(Alert alert) {
            return alert.getSeverity() != AlertSeverity.INFO;
        }
        
        @Override
        public void sendAlert(Alert alert) {
            String emoji = switch (alert.getSeverity()) {
                case CRITICAL -> "🚨";
                case WARNING -> "⚠️";
                default -> "ℹ️";
            };
            
            System.out.printf("%s [%s] %s: %s%n",
                    emoji,
                    alert.getTimestamp().format(ALERT_TIME_FORMAT),
                    alert.getRuleName(),
                    alert.getMessage());
        }
    }
    
    // === 내부 유틸리티 클래스들 ===
    
    @lombok.Data
    private static class AlertAggregator {
        private int count = 0;
        private LocalDateTime firstAlert;
        private LocalDateTime lastAlert;
        
        public void addAlert(Alert alert) {
            count++;
            if (firstAlert == null) {
                firstAlert = alert.getTimestamp();
            }
            lastAlert = alert.getTimestamp();
        }
    }
    
    // === DTO 클래스들 ===
    
    public enum AlertSeverity {
        INFO, WARNING, CRITICAL
    }
    
    public enum AlertAction {
        SENT, RESOLVED, ESCALATED
    }
    
    public enum ThresholdOperator {
        GREATER_THAN, LESS_THAN, EQUALS
    }
    
    // Alert, AlertRule, AlertState 등의 DTO는 별도 파일로 분리하는 것이 좋지만
    // 여기서는 간단히 내부 클래스로 정의
    
    @lombok.Builder
    @lombok.Data
    public static class Alert {
        private String ruleName;
        private String message;
        private AlertSeverity severity;
        private LocalDateTime timestamp;
        private String source;
        private String metricName;
        private Double currentValue;
        private Double thresholdValue;
        private boolean resolved;
    }
    
    @lombok.Builder
    @lombok.Data
    public static class AlertRule {
        private String name;
        private String description;
        private String metricName;
        private double threshold;
        private ThresholdOperator operator;
        private AlertSeverity severity;
        private int cooldownMinutes;
        private boolean enabled;
    }
    
    @lombok.Builder
    @lombok.Data
    public static class AlertState {
        private String ruleName;
        private LocalDateTime firstTriggeredTime;
        private LocalDateTime lastTriggeredTime;
        private LocalDateTime lastSentTime;
        private int count;
        private AlertSeverity severity;
        private boolean resolved;
    }
    
    @lombok.Builder
    @lombok.Data
    public static class AlertEvent {
        private Alert alert;
        private AlertAction action;
        private LocalDateTime timestamp;
    }
    
    @lombok.Builder
    @lombok.Data
    public static class AlertingStats {
        private long totalGenerated;
        private long totalSent;
        private long totalResolved;
        private int activeAlerts;
        private int alertRules;
        private int alertChannels;
        private List<AlertEvent> recentAlerts;
    }
    
    @lombok.Builder
    @lombok.Data
    public static class EscalationPolicy {
        private String name;
        private List<EscalationLevel> levels;
    }
    
    @lombok.Builder
    @lombok.Data
    public static class EscalationLevel {
        private int delayMinutes;
        private AlertSeverity severity;
        private List<String> channels;
    }
    
    // 알림 채널 인터페이스
    public interface AlertChannel {
        String getName();
        boolean shouldSendAlert(Alert alert);
        void sendAlert(Alert alert);
    }
}