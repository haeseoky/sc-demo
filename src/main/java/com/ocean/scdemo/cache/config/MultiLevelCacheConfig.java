package com.ocean.scdemo.cache.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 다단계 캐시 설정
 * L1: Caffeine (로컬 캐시) - 초고속 액세스
 * L2: Redis (글로벌 캐시) - 분산 환경 지원
 */
@Slf4j
@Configuration
@EnableCaching
public class MultiLevelCacheConfig {

    /**
     * L1 캐시 - Caffeine (로컬 캐시)
     * 특징: 매우 빠른 속도, 메모리 효율적, 애플리케이션 인스턴스별 독립적
     */
    @Bean("caffeineCacheManager")
    public CacheManager caffeineCacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        
        Cache userCache = new CaffeineCache("users",
            Caffeine.newBuilder()
                .maximumSize(10_000)              // 최대 10,000개 엔트리
                .expireAfterWrite(5, TimeUnit.MINUTES)    // 쓰기 후 5분 만료
                .expireAfterAccess(2, TimeUnit.MINUTES)   // 액세스 후 2분 만료
                .recordStats()                    // 통계 수집 활성화
                .build());

        Cache productCache = new CaffeineCache("products", 
            Caffeine.newBuilder()
                .maximumSize(5_000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .expireAfterAccess(3, TimeUnit.MINUTES)
                .recordStats()
                .build());

        Cache hotDataCache = new CaffeineCache("hotData",
            Caffeine.newBuilder()
                .maximumSize(1_000)               // 작은 크기로 자주 사용되는 데이터만
                .expireAfterWrite(1, TimeUnit.MINUTES)    // 짧은 TTL로 신선한 데이터 보장
                .recordStats()
                .build());

        cacheManager.setCaches(Arrays.asList(userCache, productCache, hotDataCache));
        return cacheManager;
    }

    /**
     * L2 캐시 - Redis (글로벌 캐시)  
     * 특징: 분산 환경 지원, 데이터 영속성, 큰 용량
     */
    @Bean("redisCacheManager")
    public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        
        // 기본 Redis 캐시 설정
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))     // 기본 30분 TTL
            .disableCachingNullValues()           // null 값 캐싱 비활성화
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer())); // JSON 직렬화

        // 캐시별 개별 설정
        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(defaultConfig)
            .withCacheConfiguration("users", defaultConfig.entryTtl(Duration.ofMinutes(15)))
            .withCacheConfiguration("products", defaultConfig.entryTtl(Duration.ofHours(1)))
            .withCacheConfiguration("sessions", defaultConfig.entryTtl(Duration.ofMinutes(60)))
            .withCacheConfiguration("longTermData", defaultConfig.entryTtl(Duration.ofHours(24)))
            .build();
    }

    /**
     * 기본 캐시 매니저 - 멀티레벨 캐시 매니저 사용
     */
    @Primary
    @Bean("multiLevelCacheManager")
    public CacheManager multiLevelCacheManager(RedisConnectionFactory redisConnectionFactory) {
        return new MultiLevelCacheManager(
            caffeineCacheManager(), 
            redisCacheManager(redisConnectionFactory)
        );
    }
}