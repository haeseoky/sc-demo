package com.ocean.scdemo.cache.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 캐시 성능 모니터링 서비스
 * Caffeine과 Redis 캐시의 통계 정보를 수집하고 분석
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheMetricsService {

    private final CacheManager multiLevelCacheManager;
    private final CacheManager caffeineCacheManager;
    private final CacheManager redisCacheManager;
    private final RedisConnectionFactory redisConnectionFactory;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 전체 캐시 성능 통계 조회
     */
    public CachePerformanceReport getAllCacheMetrics() {
        CachePerformanceReport report = new CachePerformanceReport();
        
        // Caffeine 캐시 통계
        report.setCaffeineMetrics(getCaffeineMetrics());
        
        // Redis 캐시 통계
        report.setRedisMetrics(getRedisMetrics());
        
        // 전체 성능 요약
        report.setSummary(calculatePerformanceSummary(report));
        
        return report;
    }

    /**
     * Caffeine 캐시 통계 수집
     */
    public Map<String, CaffeineMetrics> getCaffeineMetrics() {
        Map<String, CaffeineMetrics> metrics = new HashMap<>();
        
        caffeineCacheManager.getCacheNames().forEach(cacheName -> {
            org.springframework.cache.Cache cache = caffeineCacheManager.getCache(cacheName);
            if (cache instanceof CaffeineCache) {
                Cache<Object, Object> caffeineCache = ((CaffeineCache) cache).getNativeCache();
                CacheStats stats = caffeineCache.stats();
                
                CaffeineMetrics caffeineMetrics = CaffeineMetrics.builder()
                    .cacheName(cacheName)
                    .hitCount(stats.hitCount())
                    .missCount(stats.missCount())
                    .hitRate(stats.hitRate())
                    .loadCount(stats.loadCount())
                    .totalLoadTime(stats.totalLoadTime())
                    .averageLoadPenalty(stats.averageLoadPenalty())
                    .evictionCount(stats.evictionCount())
                    .estimatedSize(caffeineCache.estimatedSize())
                    .build();
                
                metrics.put(cacheName, caffeineMetrics);
                
                log.debug("Caffeine 캐시 통계 - {}: 히트율={:.2f}%, 크기={}", 
                    cacheName, stats.hitRate() * 100, caffeineCache.estimatedSize());
            }
        });
        
        return metrics;
    }

    /**
     * Redis 캐시 통계 수집
     */
    public RedisMetrics getRedisMetrics() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            Properties info = connection.info("stats");
            
            return RedisMetrics.builder()
                .keyspaceHits(getLongProperty(info, "keyspace_hits"))
                .keyspaceMisses(getLongProperty(info, "keyspace_misses"))
                .hitRate(calculateRedisHitRate(info))
                .usedMemory(getLongProperty(info, "used_memory"))
                .usedMemoryHuman(info.getProperty("used_memory_human"))
                .totalCommandsProcessed(getLongProperty(info, "total_commands_processed"))
                .instantaneousOpsPerSec(getLongProperty(info, "instantaneous_ops_per_sec"))
                .connectedClients(getLongProperty(info, "connected_clients"))
                .build();
        } catch (Exception e) {
            log.error("Redis 통계 수집 중 오류", e);
            return RedisMetrics.builder().build();
        }
    }

    /**
     * 캐시별 상세 성능 분석
     */
    public CacheDetailAnalysis analyzeCachePerformance(String cacheName) {
        CacheDetailAnalysis analysis = new CacheDetailAnalysis();
        analysis.setCacheName(cacheName);
        
        // Caffeine 분석
        org.springframework.cache.Cache caffeineCache = caffeineCacheManager.getCache(cacheName);
        if (caffeineCache instanceof CaffeineCache) {
            Cache<Object, Object> nativeCache = ((CaffeineCache) caffeineCache).getNativeCache();
            CacheStats stats = nativeCache.stats();
            
            analysis.setL1HitRate(stats.hitRate());
            analysis.setL1Size(nativeCache.estimatedSize());
            analysis.setL1EvictionCount(stats.evictionCount());
            
            // 성능 등급 판정
            if (stats.hitRate() >= 0.9) {
                analysis.setPerformanceGrade("EXCELLENT");
            } else if (stats.hitRate() >= 0.7) {
                analysis.setPerformanceGrade("GOOD");
            } else if (stats.hitRate() >= 0.5) {
                analysis.setPerformanceGrade("AVERAGE");
            } else {
                analysis.setPerformanceGrade("POOR");
            }
            
            // 최적화 추천사항
            generateOptimizationRecommendations(analysis, stats);
        }
        
        return analysis;
    }

    /**
     * 실시간 캐시 성능 모니터링
     */
    public void startCacheMonitoring() {
        // 주기적으로 캐시 통계를 로깅
        java.util.concurrent.Executors.newScheduledThreadPool(1)
            .scheduleAtFixedRate(() -> {
                try {
                    CachePerformanceReport report = getAllCacheMetrics();
                    logPerformanceReport(report);
                } catch (Exception e) {
                    log.error("캐시 모니터링 중 오류", e);
                }
            }, 0, 30, java.util.concurrent.TimeUnit.SECONDS);
    }

    /**
     * 캐시 성능 알람 체크
     */
    public void checkPerformanceAlerts() {
        getCaffeineMetrics().forEach((cacheName, metrics) -> {
            // 히트율이 50% 미만인 경우 알람
            if (metrics.getHitRate() < 0.5) {
                log.warn("캐시 성능 알람 - {}: 히트율이 낮습니다 ({:.2f}%)", 
                    cacheName, metrics.getHitRate() * 100);
            }
            
            // 평균 로드 시간이 100ms를 초과하는 경우
            if (metrics.getAverageLoadPenalty() > 100_000_000L) { // 나노초 단위
                log.warn("캐시 성능 알람 - {}: 평균 로드 시간이 깁니다 ({:.2f}ms)", 
                    cacheName, metrics.getAverageLoadPenalty() / 1_000_000.0);
            }
        });
    }

    // 헬퍼 메서드들
    private double calculateRedisHitRate(Properties info) {
        long hits = getLongProperty(info, "keyspace_hits");
        long misses = getLongProperty(info, "keyspace_misses");
        long total = hits + misses;
        return total > 0 ? (double) hits / total : 0.0;
    }

    private long getLongProperty(Properties info, String key) {
        String value = info.getProperty(key);
        return value != null ? Long.parseLong(value) : 0L;
    }

    private PerformanceSummary calculatePerformanceSummary(CachePerformanceReport report) {
        double avgHitRate = report.getCaffeineMetrics().values().stream()
            .mapToDouble(CaffeineMetrics::getHitRate)
            .average()
            .orElse(0.0);

        long totalSize = report.getCaffeineMetrics().values().stream()
            .mapToLong(CaffeineMetrics::getEstimatedSize)
            .sum();

        return PerformanceSummary.builder()
            .overallHitRate(avgHitRate)
            .totalCacheSize(totalSize)
            .redisHitRate(report.getRedisMetrics().getHitRate())
            .build();
    }

    private void generateOptimizationRecommendations(CacheDetailAnalysis analysis, CacheStats stats) {
        java.util.List<String> recommendations = new java.util.ArrayList<>();
        
        if (stats.hitRate() < 0.7) {
            recommendations.add("캐시 TTL을 늘려서 히트율을 향상시키세요");
            recommendations.add("캐시 크기를 증가시켜 더 많은 데이터를 저장하세요");
        }
        
        if (stats.evictionCount() > stats.hitCount() * 0.1) {
            recommendations.add("캐시 최대 크기를 늘려서 eviction을 줄이세요");
        }
        
        if (stats.averageLoadPenalty() > 100_000_000L) {
            recommendations.add("데이터 로딩 성능을 최적화하세요");
        }
        
        analysis.setRecommendations(recommendations);
    }

    private void logPerformanceReport(CachePerformanceReport report) {
        StringBuilder sb = new StringBuilder("\n=== 캐시 성능 리포트 ===\n");
        
        report.getCaffeineMetrics().forEach((name, metrics) -> {
            sb.append(String.format("L1 캐시 [%s]: 히트율=%.2f%%, 크기=%d, evictions=%d\n",
                name, metrics.getHitRate() * 100, metrics.getEstimatedSize(), metrics.getEvictionCount()));
        });
        
        sb.append(String.format("Redis: 히트율=%.2f%%, 메모리=%s, QPS=%d\n",
            report.getRedisMetrics().getHitRate() * 100,
            report.getRedisMetrics().getUsedMemoryHuman(),
            report.getRedisMetrics().getInstantaneousOpsPerSec()));
        
        log.info(sb.toString());
    }

    // DTO 클래스들
    @lombok.Data
    public static class CachePerformanceReport {
        private Map<String, CaffeineMetrics> caffeineMetrics;
        private RedisMetrics redisMetrics;
        private PerformanceSummary summary;
    }

    @lombok.Builder
    @lombok.Data
    public static class CaffeineMetrics {
        private String cacheName;
        private long hitCount;
        private long missCount;
        private double hitRate;
        private long loadCount;
        private long totalLoadTime;
        private double averageLoadPenalty;
        private long evictionCount;
        private long estimatedSize;
    }

    @lombok.Builder
    @lombok.Data
    public static class RedisMetrics {
        private long keyspaceHits;
        private long keyspaceMisses;
        private double hitRate;
        private long usedMemory;
        private String usedMemoryHuman;
        private long totalCommandsProcessed;
        private long instantaneousOpsPerSec;
        private long connectedClients;
    }

    @lombok.Builder
    @lombok.Data
    public static class PerformanceSummary {
        private double overallHitRate;
        private long totalCacheSize;
        private double redisHitRate;
    }

    @lombok.Data
    public static class CacheDetailAnalysis {
        private String cacheName;
        private double l1HitRate;
        private long l1Size;
        private long l1EvictionCount;
        private String performanceGrade;
        private java.util.List<String> recommendations;
    }
}