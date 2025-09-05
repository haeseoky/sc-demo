package com.ocean.scdemo.cache.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 고성능 캐시 서비스
 * 다단계 캐시 전략을 활용한 최적화된 데이터 조회 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HighPerformanceCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 사용자 정보 조회 - 멀티레벨 캐시 적용
     * L1(Caffeine) -> L2(Redis) -> DB 순서로 조회
     */
    @Cacheable(value = "users", key = "#userId", cacheManager = "multiLevelCacheManager")
    public UserData getUserData(String userId) {
        log.info("DB에서 사용자 조회: {}", userId);
        
        // 실제 DB 조회 시뮬레이션 (느린 작업)
        simulateSlowDatabaseQuery(100);
        
        return UserData.builder()
            .id(userId)
            .name("사용자" + userId)
            .email(userId + "@example.com")
            .score(Math.random() * 1000)
            .build();
    }

    /**
     * 상품 정보 조회 - 조건부 캐싱
     */
    @Cacheable(value = "products", key = "#productId", 
               condition = "#productId != null", 
               unless = "#result.price < 100",
               cacheManager = "multiLevelCacheManager")
    public ProductData getProductData(String productId) {
        log.info("DB에서 상품 조회: {}", productId);
        
        simulateSlowDatabaseQuery(80);
        
        return ProductData.builder()
            .id(productId)
            .name("상품" + productId)
            .price(Math.random() * 10000)
            .stock((int)(Math.random() * 100))
            .build();
    }

    /**
     * 핫 데이터 조회 - 매우 짧은 TTL로 신선한 데이터 보장
     */
    @Cacheable(value = "hotData", key = "#dataKey", cacheManager = "multiLevelCacheManager")
    public HotData getHotData(String dataKey) {
        log.info("실시간 데이터 조회: {}", dataKey);
        
        // 실시간 데이터는 빠르게 조회되어야 함
        simulateSlowDatabaseQuery(20);
        
        return HotData.builder()
            .key(dataKey)
            .value("실시간 값: " + System.currentTimeMillis())
            .timestamp(System.currentTimeMillis())
            .build();
    }

    /**
     * 사용자 데이터 업데이트 - 캐시 갱신
     */
    @CachePut(value = "users", key = "#userData.id", cacheManager = "multiLevelCacheManager")
    public UserData updateUserData(UserData userData) {
        log.info("사용자 데이터 업데이트: {}", userData.getId());
        
        // DB 업데이트 시뮬레이션
        simulateSlowDatabaseQuery(50);
        
        return userData;
    }

    /**
     * 사용자 데이터 삭제 - 캐시 제거
     */
    @CacheEvict(value = "users", key = "#userId", cacheManager = "multiLevelCacheManager")
    public void deleteUserData(String userId) {
        log.info("사용자 데이터 삭제: {}", userId);
        // DB 삭제 로직
    }

    /**
     * 모든 사용자 캐시 제거
     */
    @CacheEvict(value = "users", allEntries = true, cacheManager = "multiLevelCacheManager")
    public void clearAllUserCache() {
        log.info("모든 사용자 캐시 제거");
    }

    /**
     * 비동기 캐시 예열 (Cache Warming)
     */
    public CompletableFuture<Void> warmupCache() {
        return CompletableFuture.runAsync(() -> {
            log.info("캐시 예열 시작");
            
            // 주요 사용자 데이터 미리 로드
            for (int i = 1; i <= 100; i++) {
                getUserData("user" + i);
            }
            
            // 주요 상품 데이터 미리 로드
            for (int i = 1; i <= 50; i++) {
                getProductData("product" + i);
            }
            
            log.info("캐시 예열 완료");
        });
    }

    /**
     * 캐시 미스 시 비동기 백그라운드 리프레시
     */
    public <T> T getWithAsyncRefresh(String cacheKey, String cacheName, Supplier<T> dataSupplier) {
        // 먼저 캐시에서 조회
        T cachedValue = (T) redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedValue != null) {
            // 캐시 히트 - 백그라운드에서 비동기 갱신 (선택적)
            CompletableFuture.runAsync(() -> {
                try {
                    T freshData = dataSupplier.get();
                    redisTemplate.opsForValue().set(cacheKey, freshData, 10, TimeUnit.MINUTES);
                    log.debug("백그라운드 캐시 갱신 완료: {}", cacheKey);
                } catch (Exception e) {
                    log.warn("백그라운드 캐시 갱신 실패: {}", cacheKey, e);
                }
            });
            return cachedValue;
        }
        
        // 캐시 미스 - 데이터 조회 후 캐시 저장
        T freshData = dataSupplier.get();
        redisTemplate.opsForValue().set(cacheKey, freshData, 10, TimeUnit.MINUTES);
        return freshData;
    }

    /**
     * 배치 캐시 조회 - 여러 키를 한 번에 조회
     */
    public <T> java.util.Map<String, T> getBatch(java.util.List<String> keys, String cachePrefix, 
                                                   java.util.function.Function<java.util.List<String>, java.util.Map<String, T>> dataLoader) {
        
        // Redis에서 배치 조회
        java.util.List<String> fullKeys = keys.stream()
            .map(key -> cachePrefix + ":" + key)
            .toList();
        
        java.util.List<Object> cachedValues = redisTemplate.opsForValue().multiGet(fullKeys);
        java.util.Map<String, T> result = new java.util.HashMap<>();
        java.util.List<String> missedKeys = new java.util.ArrayList<>();
        
        for (int i = 0; i < keys.size(); i++) {
            if (cachedValues.get(i) != null) {
                result.put(keys.get(i), (T) cachedValues.get(i));
            } else {
                missedKeys.add(keys.get(i));
            }
        }
        
        // 캐시 미스된 키들을 DB에서 조회
        if (!missedKeys.isEmpty()) {
            java.util.Map<String, T> freshData = dataLoader.apply(missedKeys);
            
            // 새로 조회한 데이터를 캐시에 저장
            freshData.forEach((key, value) -> {
                redisTemplate.opsForValue().set(cachePrefix + ":" + key, value, 10, TimeUnit.MINUTES);
                result.put(key, value);
            });
        }
        
        return result;
    }

    /**
     * DB 조회 시뮬레이션 (지연시간 추가)
     */
    private void simulateSlowDatabaseQuery(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // DTO 클래스들
    @lombok.Builder
    @lombok.Data
    public static class UserData {
        private String id;
        private String name;
        private String email;
        private double score;
    }

    @lombok.Builder
    @lombok.Data
    public static class ProductData {
        private String id;
        private String name;
        private double price;
        private int stock;
    }

    @lombok.Builder
    @lombok.Data
    public static class HotData {
        private String key;
        private String value;
        private long timestamp;
    }
}