package com.ocean.scdemo.cache.service;

import com.ocean.scdemo.cache.config.DualCacheConfig;
import com.ocean.scdemo.cache.model.CacheableData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 듀얼 캐시 서비스
 * 
 * 기능:
 * - 런타임에 캐시 타입 선택 가능
 * - Caffeine, EhCache, MultiLevel 캐시 지원
 * - 캐시별 성능 비교 및 통계
 * - 동적 캐시 전환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DualCacheService {

    private final DualCacheConfig.CacheSelector cacheSelector;
    private final Map<String, Long> accessTimes = new ConcurrentHashMap<>();
    private final Random random = new Random();

    /**
     * 지정된 캐시 타입으로 사용자 데이터 조회
     */
    public CacheableData getUserData(String userId, String cacheType) {
        long startTime = System.nanoTime();
        
        try {
            CacheManager cacheManager = cacheSelector.getCacheManager(cacheType);
            String cacheName = getCacheName(cacheType, "users");
            Cache cache = cacheManager.getCache(cacheName);
            
            if (cache == null) {
                log.warn("⚠️ 캐시를 찾을 수 없음: {}", cacheName);
                return generateUserData(userId);
            }
            
            // 캐시에서 조회
            Cache.ValueWrapper valueWrapper = cache.get(userId);
            CacheableData userData;
            
            if (valueWrapper != null) {
                // 캐시 히트
                userData = (CacheableData) valueWrapper.get();
                long elapsedTime = System.nanoTime() - startTime;
                recordAccessTime(cacheType + "-hit", elapsedTime);
                
                log.debug("🎯 캐시 히트 - {} 사용: userId={}, 응답시간={}ns", 
                         cacheType, userId, elapsedTime);
                
            } else {
                // 캐시 미스 - 새 데이터 생성 및 캐싱
                userData = generateUserData(userId);
                cache.put(userId, userData);
                
                long elapsedTime = System.nanoTime() - startTime;
                recordAccessTime(cacheType + "-miss", elapsedTime);
                
                log.debug("💾 캐시 미스 - {} 사용: userId={}, 응답시간={}ns", 
                         cacheType, userId, elapsedTime);
            }
            
            return userData;
            
        } catch (Exception e) {
            log.error("❌ {} 캐시 조회 실패: userId={}", cacheType, userId, e);
            return generateUserData(userId);
        }
    }

    /**
     * 지정된 캐시 타입으로 상품 데이터 조회
     */
    public CacheableData getProductData(String productId, String cacheType) {
        long startTime = System.nanoTime();
        
        try {
            CacheManager cacheManager = cacheSelector.getCacheManager(cacheType);
            String cacheName = getCacheName(cacheType, "products");
            Cache cache = cacheManager.getCache(cacheName);
            
            if (cache == null) {
                log.warn("⚠️ 캐시를 찾을 수 없음: {}", cacheName);
                return generateProductData(productId);
            }
            
            Cache.ValueWrapper valueWrapper = cache.get(productId);
            CacheableData productData;
            
            if (valueWrapper != null) {
                productData = (CacheableData) valueWrapper.get();
                long elapsedTime = System.nanoTime() - startTime;
                recordAccessTime(cacheType + "-product-hit", elapsedTime);
                
            } else {
                productData = generateProductData(productId);
                cache.put(productId, productData);
                
                long elapsedTime = System.nanoTime() - startTime;
                recordAccessTime(cacheType + "-product-miss", elapsedTime);
            }
            
            return productData;
            
        } catch (Exception e) {
            log.error("❌ {} 캐시 조회 실패: productId={}", cacheType, productId, e);
            return generateProductData(productId);
        }
    }

    /**
     * 캐시 성능 비교 테스트
     */
    public Map<String, Object> performanceComparison(String dataId) {
        Map<String, Object> results = new ConcurrentHashMap<>();
        
        // 각 캐시 타입별로 동일한 데이터 조회
        for (String cacheType : cacheSelector.getAvailableCacheTypes()) {
            long totalTime = 0;
            int iterations = 100;
            
            // 데이터 준비 (캐시 워밍업)
            getUserData(dataId, cacheType);
            
            // 성능 측정
            for (int i = 0; i < iterations; i++) {
                long startTime = System.nanoTime();
                getUserData(dataId, cacheType);
                totalTime += (System.nanoTime() - startTime);
            }
            
            double avgTimeNs = (double) totalTime / iterations;
            double avgTimeMicros = avgTimeNs / 1000.0;
            
            results.put(cacheType + "_avg_time_ns", avgTimeNs);
            results.put(cacheType + "_avg_time_micros", avgTimeMicros);
            
            log.info("📊 {} 캐시 평균 응답시간: {:.2f}μs ({:.0f}ns)", 
                    cacheType, avgTimeMicros, avgTimeNs);
        }
        
        return results;
    }

    /**
     * 모든 캐시 지우기
     */
    public void clearAllCaches() {
        for (String cacheType : cacheSelector.getAvailableCacheTypes()) {
            try {
                CacheManager cacheManager = cacheSelector.getCacheManager(cacheType);
                cacheManager.getCacheNames().forEach(cacheName -> {
                    Cache cache = cacheManager.getCache(cacheName);
                    if (cache != null) {
                        cache.clear();
                        log.debug("🗑️ {} 캐시 클리어: {}", cacheType, cacheName);
                    }
                });
            } catch (Exception e) {
                log.error("❌ {} 캐시 클리어 실패", cacheType, e);
            }
        }
        
        // 통계 초기화
        accessTimes.clear();
        log.info("✅ 모든 캐시 및 통계 초기화 완료");
    }

    /**
     * 캐시 통계 조회
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        // 액세스 시간 통계
        stats.put("accessTimes", Map.copyOf(accessTimes));
        
        // 캐시별 특성 정보
        Map<String, String> characteristics = new ConcurrentHashMap<>();
        for (String cacheType : cacheSelector.getAvailableCacheTypes()) {
            characteristics.put(cacheType, cacheSelector.getCacheCharacteristics(cacheType));
        }
        stats.put("characteristics", characteristics);
        
        // 사용 가능한 캐시 목록
        stats.put("availableCacheTypes", cacheSelector.getAvailableCacheTypes());
        
        return stats;
    }

    /**
     * 캐시 타입에 따른 캐시명 매핑
     */
    private String getCacheName(String cacheType, String baseCache) {
        return switch (cacheType.toLowerCase()) {
            case "ehcache" -> "ehcache-" + baseCache;
            case "caffeine", "multilevel" -> baseCache;
            default -> baseCache;
        };
    }

    /**
     * 액세스 시간 기록
     */
    private void recordAccessTime(String key, long timeNs) {
        accessTimes.put(key, timeNs);
    }

    /**
     * 테스트용 사용자 데이터 생성
     */
    private CacheableData generateUserData(String userId) {
        // 실제 DB 조회를 시뮬레이션 (약간의 지연)
        try {
            Thread.sleep(random.nextInt(50) + 10); // 10-60ms 랜덤 지연
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return CacheableData.builder()
                .id(userId)
                .name("사용자" + userId)
                .email(userId + "@example.com")
                .score(random.nextDouble() * 1000)
                .createdAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .metadata(Map.of(
                    "accessCount", random.nextInt(100),
                    "lastLogin", LocalDateTime.now().minusDays(random.nextInt(30)).toString(),
                    "preferences", Map.of("theme", "dark", "language", "ko")
                ))
                .build();
    }

    /**
     * 테스트용 상품 데이터 생성
     */
    private CacheableData generateProductData(String productId) {
        try {
            Thread.sleep(random.nextInt(30) + 5); // 5-35ms 랜덤 지연
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        String[] categories = {"전자제품", "의류", "책", "음식", "스포츠용품"};
        
        return CacheableData.builder()
                .id(productId)
                .name("상품" + productId)
                .email(null) // 상품에는 이메일 없음
                .score(random.nextDouble() * 5.0) // 평점 (0-5)
                .createdAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .metadata(Map.of(
                    "category", categories[random.nextInt(categories.length)],
                    "price", random.nextInt(100000) + 1000,
                    "stock", random.nextInt(1000),
                    "reviews", random.nextInt(500)
                ))
                .build();
    }
}