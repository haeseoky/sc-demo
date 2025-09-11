package com.ocean.scdemo.redispubsub.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Redis Pub/Sub 최적화 설정
 * 
 * 핵심 기능:
 * - 고성능 메시지 직렬화/역직렬화 (Jackson)
 * - 패턴 기반 토픽 구독
 * - 비동기 메시지 처리를 위한 스레드 풀 최적화
 * - Connection Pool 관리
 * - 에러 복구 및 재시도 메커니즘
 */
@Slf4j
@Configuration
public class RedisPubSubConfig {

    /**
     * Redis Pub/Sub 전용 RedisTemplate 설정
     * 메시지 발행을 위한 최적화된 설정
     */
    @Bean("redisPubSubTemplate")
    public RedisTemplate<String, Object> redisPubSubTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 키 직렬화 - String 사용 (토픽명)
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // 값 직렬화 - JSON 사용 (메시지 객체)
        ObjectMapper objectMapper = createOptimizedObjectMapper();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        // 기본 직렬화 설정
        template.setDefaultSerializer(jsonSerializer);
        template.setEnableTransactionSupport(false); // Pub/Sub는 트랜잭션 불필요
        
        template.afterPropertiesSet();
        
        log.info("Redis Pub/Sub Template 초기화 완료");
        return template;
    }

    /**
     * 메시지 리스너 컨테이너 설정
     * 다중 구독자 및 패턴 매칭을 위한 고성능 설정
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            Executor pubSubTaskExecutor) {
        
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        
        // 비동기 처리를 위한 전용 TaskExecutor 설정
        container.setTaskExecutor(pubSubTaskExecutor);
        
        // 구독 타임아웃은 Spring Data Redis 최신 버전에서 자동 관리됨
        
        // 에러 핸들러 설정
        container.setErrorHandler(throwable -> {
            log.error("Redis Pub/Sub 리스너 에러 발생", throwable);
            // 에러 발생 시 재연결 시도를 위한 로직
            // 실제 환경에서는 알림 시스템 연동 필요
        });
        
        // Recovery Interval 설정 (1초)
        container.setRecoveryInterval(1000);
        
        log.info("Redis Message Listener Container 초기화 완료");
        return container;
    }

    /**
     * Pub/Sub 전용 스레드 풀 TaskExecutor
     * 메시지 처리 성능 최적화를 위한 설정
     */
    @Bean("pubSubTaskExecutor")
    public Executor pubSubTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 코어 스레드 수 (기본 처리 용량)
        executor.setCorePoolSize(5);
        
        // 최대 스레드 수 (피크 타임 처리)
        executor.setMaxPoolSize(20);
        
        // 큐 용량 (대기 메시지 수)
        executor.setQueueCapacity(100);
        
        // 스레드 이름 prefix
        executor.setThreadNamePrefix("RedisPubSub-");
        
        // 스레드 유지 시간 (60초)
        executor.setKeepAliveSeconds(60);
        
        // 애플리케이션 종료 시 실행 중인 작업 완료 대기
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        
        // 거부 정책 (큐가 가득 찬 경우 호출 스레드에서 실행)
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.initialize();
        
        log.info("Pub/Sub TaskExecutor 초기화 완료 - Core: {}, Max: {}, Queue: {}", 
                 executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }

    /**
     * 채널별 토픽 정의 - 구독 패턴 설정
     */
    public static class Topics {
        // 채팅 메시지 토픽
        public static final PatternTopic CHAT_MESSAGES = new PatternTopic("chat:*");
        public static final PatternTopic CHAT_ROOM_MESSAGES = new PatternTopic("chat:room:*");
        
        // 알림 토픽
        public static final PatternTopic USER_NOTIFICATIONS = new PatternTopic("notification:user:*");
        public static final PatternTopic SYSTEM_NOTIFICATIONS = new PatternTopic("notification:system:*");
        public static final PatternTopic BROADCAST_NOTIFICATIONS = new PatternTopic("notification:broadcast");
        
        // 이벤트 스트리밍 토픽
        public static final PatternTopic USER_EVENTS = new PatternTopic("event:user:*");
        public static final PatternTopic SYSTEM_EVENTS = new PatternTopic("event:system:*");
        
        // 상태 업데이트 토픽
        public static final PatternTopic STATUS_UPDATES = new PatternTopic("status:*");
        public static final PatternTopic HEALTH_CHECKS = new PatternTopic("health:*");
        
        // 메트릭스 및 모니터링 토픽
        public static final PatternTopic METRICS = new PatternTopic("metrics:*");
        public static final PatternTopic PERFORMANCE = new PatternTopic("performance:*");
    }

    /**
     * 채널 이름 상수 정의
     */
    public static class Channels {
        // 채팅 채널
        public static final String CHAT_GLOBAL = "chat:global";
        public static final String CHAT_ROOM_PREFIX = "chat:room:";
        
        // 알림 채널  
        public static final String NOTIFICATION_USER_PREFIX = "notification:user:";
        public static final String NOTIFICATION_SYSTEM = "notification:system:alerts";
        public static final String NOTIFICATION_BROADCAST = "notification:broadcast";
        
        // 이벤트 채널
        public static final String EVENT_USER_PREFIX = "event:user:";
        public static final String EVENT_SYSTEM = "event:system:activities";
        
        // 상태 채널
        public static final String STATUS_ONLINE = "status:online";
        public static final String STATUS_OFFLINE = "status:offline";
        
        // 헬스체크 채널
        public static final String HEALTH_CHECK = "health:check";
        public static final String HEALTH_STATUS = "health:status";
        
        // 메트릭스 채널
        public static final String METRICS_PERFORMANCE = "metrics:performance";
        public static final String METRICS_USAGE = "metrics:usage";
        
        /**
         * 동적 채널 이름 생성 헬퍼 메서드
         */
        public static String chatRoom(String roomId) {
            return CHAT_ROOM_PREFIX + roomId;
        }
        
        public static String userNotification(String userId) {
            return NOTIFICATION_USER_PREFIX + userId;
        }
        
        public static String userEvent(String userId) {
            return EVENT_USER_PREFIX + userId;
        }
    }

    /**
     * JSON 직렬화를 위한 최적화된 ObjectMapper 설정
     */
    private ObjectMapper createOptimizedObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Java 8 Time API 지원
        objectMapper.registerModule(new JavaTimeModule());
        
        // 타입 정보 포함 (다형성 지원)
        objectMapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL
        );
        
        // 알 수 없는 속성 무시
        objectMapper.configure(
            com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, 
            false
        );
        
        // null 값 무시
        objectMapper.setSerializationInclusion(
            com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
        );
        
        return objectMapper;
    }
}