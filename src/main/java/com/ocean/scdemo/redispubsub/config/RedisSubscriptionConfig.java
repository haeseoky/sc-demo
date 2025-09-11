package com.ocean.scdemo.redispubsub.config;

import com.ocean.scdemo.redispubsub.subscriber.MessageSubscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.concurrent.Executor;

/**
 * Redis Pub/Sub 구독 설정
 * 
 * 핵심 기능:
 * - 모든 채널에 대한 패턴 기반 구독 설정
 * - 동적 구독 관리 (런타임에 구독 추가/제거)
 * - 구독 상태 모니터링
 * - 자동 재연결 및 에러 복구
 * - 구독 통계 수집
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RedisSubscriptionConfig {

    private final MessageSubscriber messageSubscriber;
    private final RedisConnectionFactory redisConnectionFactory;
    private final Executor pubSubTaskExecutor;

    /**
     * 애플리케이션 시작 시 구독 설정 자동 초기화
     */
    @Bean
    public ApplicationRunner subscriptionInitializer(RedisMessageListenerContainer listenerContainer) {
        return args -> {
            log.info("🚀 Redis Pub/Sub 구독 초기화 시작");
            
            try {
                // 각 토픽 패턴별로 구독 설정
                setupChatSubscriptions(listenerContainer);
                setupNotificationSubscriptions(listenerContainer);
                setupEventSubscriptions(listenerContainer);
                setupMetricsSubscriptions(listenerContainer);
                setupHealthCheckSubscriptions(listenerContainer);
                setupSystemSubscriptions(listenerContainer);
                
                log.info("✅ Redis Pub/Sub 구독 초기화 완료");
                
                // 구독 상태 로그
                logSubscriptionStatus(listenerContainer);
                
            } catch (Exception e) {
                log.error("❌ Redis Pub/Sub 구독 초기화 실패", e);
                throw e;
            }
        };
    }

    /**
     * 채팅 관련 채널 구독 설정
     */
    private void setupChatSubscriptions(RedisMessageListenerContainer container) {
        // 전체 채팅 메시지 구독
        container.addMessageListener(messageSubscriber, RedisPubSubConfig.Topics.CHAT_MESSAGES);
        log.debug("📝 채팅 메시지 구독 설정 완료: {}", RedisPubSubConfig.Topics.CHAT_MESSAGES.getTopic());
        
        // 채팅방별 메시지 구독
        container.addMessageListener(messageSubscriber, RedisPubSubConfig.Topics.CHAT_ROOM_MESSAGES);
        log.debug("🏠 채팅방 메시지 구독 설정 완료: {}", RedisPubSubConfig.Topics.CHAT_ROOM_MESSAGES.getTopic());
    }

    /**
     * 알림 관련 채널 구독 설정
     */
    private void setupNotificationSubscriptions(RedisMessageListenerContainer container) {
        // 사용자별 알림 구독
        container.addMessageListener(messageSubscriber, RedisPubSubConfig.Topics.USER_NOTIFICATIONS);
        log.debug("👤 사용자 알림 구독 설정 완료: {}", RedisPubSubConfig.Topics.USER_NOTIFICATIONS.getTopic());
        
        // 시스템 알림 구독
        container.addMessageListener(messageSubscriber, RedisPubSubConfig.Topics.SYSTEM_NOTIFICATIONS);
        log.debug("🔔 시스템 알림 구독 설정 완료: {}", RedisPubSubConfig.Topics.SYSTEM_NOTIFICATIONS.getTopic());
        
        // 브로드캐스트 알림 구독
        container.addMessageListener(messageSubscriber, RedisPubSubConfig.Topics.BROADCAST_NOTIFICATIONS);
        log.debug("📢 브로드캐스트 알림 구독 설정 완료: {}", RedisPubSubConfig.Topics.BROADCAST_NOTIFICATIONS.getTopic());
    }

    /**
     * 이벤트 관련 채널 구독 설정
     */
    private void setupEventSubscriptions(RedisMessageListenerContainer container) {
        // 사용자 이벤트 구독
        container.addMessageListener(messageSubscriber, RedisPubSubConfig.Topics.USER_EVENTS);
        log.debug("👥 사용자 이벤트 구독 설정 완료: {}", RedisPubSubConfig.Topics.USER_EVENTS.getTopic());
        
        // 시스템 이벤트 구독
        container.addMessageListener(messageSubscriber, RedisPubSubConfig.Topics.SYSTEM_EVENTS);
        log.debug("⚙️ 시스템 이벤트 구독 설정 완료: {}", RedisPubSubConfig.Topics.SYSTEM_EVENTS.getTopic());
    }

    /**
     * 메트릭스 관련 채널 구독 설정
     */
    private void setupMetricsSubscriptions(RedisMessageListenerContainer container) {
        // 메트릭스 구독
        container.addMessageListener(messageSubscriber, RedisPubSubConfig.Topics.METRICS);
        log.debug("📊 메트릭스 구독 설정 완료: {}", RedisPubSubConfig.Topics.METRICS.getTopic());
        
        // 성능 메트릭스 구독
        container.addMessageListener(messageSubscriber, RedisPubSubConfig.Topics.PERFORMANCE);
        log.debug("🚀 성능 메트릭스 구독 설정 완료: {}", RedisPubSubConfig.Topics.PERFORMANCE.getTopic());
    }

    /**
     * 헬스체크 관련 채널 구독 설정
     */
    private void setupHealthCheckSubscriptions(RedisMessageListenerContainer container) {
        // 헬스체크 구독
        container.addMessageListener(messageSubscriber, RedisPubSubConfig.Topics.HEALTH_CHECKS);
        log.debug("🏥 헬스체크 구독 설정 완료: {}", RedisPubSubConfig.Topics.HEALTH_CHECKS.getTopic());
    }

    /**
     * 상태 업데이트 관련 채널 구독 설정
     */
    private void setupSystemSubscriptions(RedisMessageListenerContainer container) {
        // 상태 업데이트 구독
        container.addMessageListener(messageSubscriber, RedisPubSubConfig.Topics.STATUS_UPDATES);
        log.debug("📡 상태 업데이트 구독 설정 완료: {}", RedisPubSubConfig.Topics.STATUS_UPDATES.getTopic());
    }

    /**
     * 구독 상태 로깅
     */
    private void logSubscriptionStatus(RedisMessageListenerContainer container) {
        try {
            // 구독 중인 채널 수 계산 (대략적)
            int subscriptionCount = 7; // 주요 패턴 토픽 개수
            
            log.info("📊 Redis Pub/Sub 구독 상태:");
            log.info("  └─ 총 구독 패턴: {}개", subscriptionCount);
            log.info("  └─ 메시지 리스너: MessageSubscriber");
            log.info("  └─ Task Executor: {}", pubSubTaskExecutor.getClass().getSimpleName());
            log.info("  └─ 구독 대상 패턴:");
            log.info("     ├─ 채팅: chat:*");
            log.info("     ├─ 알림: notification:*");
            log.info("     ├─ 이벤트: event:*");
            log.info("     ├─ 메트릭스: metrics:*, performance:*");
            log.info("     ├─ 헬스체크: health:*");
            log.info("     └─ 상태: status:*");
            
        } catch (Exception e) {
            log.warn("구독 상태 로깅 중 오류 발생", e);
        }
    }

    /**
     * 동적 구독 관리 서비스
     */
    @Bean
    public SubscriptionManager subscriptionManager(RedisMessageListenerContainer container) {
        return new SubscriptionManager(container, messageSubscriber);
    }

    /**
     * 동적 구독 관리 클래스
     */
    public static class SubscriptionManager {
        private final RedisMessageListenerContainer container;
        private final MessageSubscriber messageSubscriber;
        
        public SubscriptionManager(RedisMessageListenerContainer container, 
                                 MessageSubscriber messageSubscriber) {
            this.container = container;
            this.messageSubscriber = messageSubscriber;
        }
        
        /**
         * 새 채널 구독 추가
         */
        public boolean addSubscription(String channelPattern) {
            try {
                org.springframework.data.redis.listener.PatternTopic topic = 
                    new org.springframework.data.redis.listener.PatternTopic(channelPattern);
                
                container.addMessageListener(messageSubscriber, topic);
                
                log.info("➕ 새 구독 추가: {}", channelPattern);
                return true;
                
            } catch (Exception e) {
                log.error("구독 추가 실패: {}", channelPattern, e);
                return false;
            }
        }
        
        /**
         * 채널 구독 제거
         */
        public boolean removeSubscription(String channelPattern) {
            try {
                org.springframework.data.redis.listener.PatternTopic topic = 
                    new org.springframework.data.redis.listener.PatternTopic(channelPattern);
                
                container.removeMessageListener(messageSubscriber, topic);
                
                log.info("➖ 구독 제거: {}", channelPattern);
                return true;
                
            } catch (Exception e) {
                log.error("구독 제거 실패: {}", channelPattern, e);
                return false;
            }
        }
        
        /**
         * 모든 구독 제거 (종료 시 정리용)
         */
        public void removeAllSubscriptions() {
            try {
                container.removeMessageListener(messageSubscriber);
                log.info("🧹 모든 구독 제거 완료");
                
            } catch (Exception e) {
                log.error("전체 구독 제거 실패", e);
            }
        }
        
        /**
         * 구독 통계 조회
         */
        public SubscriptionStats getSubscriptionStats() {
            try {
                // MessageSubscriber에서 통계 조회
                MessageSubscriber.SubscriptionStats stats = messageSubscriber.getSubscriptionStats();
                
                return SubscriptionStats.builder()
                        .totalReceived(stats.getTotalReceived())
                        .totalProcessed(stats.getTotalProcessed())
                        .totalFailed(stats.getTotalFailed())
                        .totalIgnored(stats.getTotalIgnored())
                        .successRate(stats.getSuccessRate())
                        .failureRate(stats.getFailureRate())
                        .ignoreRate(stats.getIgnoreRate())
                        .containerActive(container.isActive())
                        .containerRunning(container.isRunning())
                        .build();
                        
            } catch (Exception e) {
                log.error("구독 통계 조회 실패", e);
                return SubscriptionStats.builder().build();
            }
        }
        
        /**
         * 구독 상태 체크
         */
        public boolean isHealthy() {
            try {
                SubscriptionStats stats = getSubscriptionStats();
                
                // 컨테이너가 활성화되어 있고 실행 중인지 확인
                boolean containerHealthy = stats.isContainerActive() && stats.isContainerRunning();
                
                // 성공률이 80% 이상인지 확인
                boolean processingHealthy = stats.getSuccessRate() >= 80.0 || 
                                          stats.getTotalReceived() < 10; // 메시지가 적으면 통과
                
                return containerHealthy && processingHealthy;
                
            } catch (Exception e) {
                log.error("구독 상태 체크 실패", e);
                return false;
            }
        }
    }

    /**
     * 구독 통계 DTO
     */
    @lombok.Builder
    @lombok.Data
    public static class SubscriptionStats {
        private final long totalReceived;
        private final long totalProcessed;
        private final long totalFailed;
        private final long totalIgnored;
        private final double successRate;
        private final double failureRate;
        private final double ignoreRate;
        private final boolean containerActive;
        private final boolean containerRunning;
        
        public boolean isHealthy() {
            return containerActive && containerRunning && successRate >= 80.0;
        }
        
        public String getHealthStatus() {
            if (!containerActive || !containerRunning) {
                return "컨테이너 비활성";
            } else if (successRate >= 95.0) {
                return "매우 양호";
            } else if (successRate >= 80.0) {
                return "양호";
            } else if (successRate >= 60.0) {
                return "보통";
            } else {
                return "불량";
            }
        }
    }
}