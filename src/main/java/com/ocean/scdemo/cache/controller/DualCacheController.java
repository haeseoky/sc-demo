package com.ocean.scdemo.cache.controller;

import com.ocean.scdemo.cache.config.DualCacheConfig;
import com.ocean.scdemo.cache.model.CacheableData;
import com.ocean.scdemo.cache.service.DualCacheService;
import com.ocean.scdemo.config.model.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 듀얼 캐시 시스템 테스트 컨트롤러
 * 
 * 기능:
 * - Caffeine vs EhCache vs MultiLevel 캐시 비교
 * - 런타임 캐시 선택 테스트
 * - 캐시 성능 측정 및 통계
 * - 캐시 관리 (클리어, 통계 조회)
 */
@Slf4j
@Tag(name = "Dual Cache API", description = "듀얼 캐시 시스템 테스트 및 비교 API")
@RestController
@RequestMapping("/api/cache/dual")
@RequiredArgsConstructor
public class DualCacheController {

    private final DualCacheService dualCacheService;
    private final DualCacheConfig.CacheSelector cacheSelector;
    private final DualCacheConfig.CachePerformanceGuide performanceGuide;

    /**
     * 캐시 타입별 사용자 데이터 조회
     */
    @Operation(summary = "캐시 타입별 사용자 데이터 조회", 
               description = "지정된 캐시 타입(caffeine, ehcache, multilevel)으로 사용자 데이터를 조회합니다")
    @GetMapping("/users/{userId}")
    public ResponseEntity<CommonResponse<CacheableData>> getUserData(
            @Parameter(description = "사용자 ID") @PathVariable String userId,
            @Parameter(description = "캐시 타입", example = "caffeine") 
            @RequestParam(defaultValue = "caffeine") String cacheType) {
        
        log.info("📋 사용자 데이터 조회 요청: userId={}, cacheType={}", userId, cacheType);
        
        CacheableData userData = dualCacheService.getUserData(userId, cacheType);
        
        return ResponseEntity.ok(CommonResponse.success(userData));
    }

    /**
     * 캐시 타입별 상품 데이터 조회
     */
    @Operation(summary = "캐시 타입별 상품 데이터 조회",
               description = "지정된 캐시 타입으로 상품 데이터를 조회합니다")
    @GetMapping("/products/{productId}")
    public ResponseEntity<CommonResponse<CacheableData>> getProductData(
            @Parameter(description = "상품 ID") @PathVariable String productId,
            @Parameter(description = "캐시 타입") 
            @RequestParam(defaultValue = "caffeine") String cacheType) {
        
        log.info("🛍️ 상품 데이터 조회 요청: productId={}, cacheType={}", productId, cacheType);
        
        CacheableData productData = dualCacheService.getProductData(productId, cacheType);
        
        return ResponseEntity.ok(CommonResponse.success(productData));
    }

    /**
     * 캐시 성능 비교 테스트
     */
    @Operation(summary = "캐시 성능 비교", 
               description = "모든 캐시 타입의 성능을 비교 측정합니다")
    @PostMapping("/performance-test")
    public ResponseEntity<CommonResponse<Map<String, Object>>> performanceTest(
            @Parameter(description = "테스트 데이터 ID") 
            @RequestParam(defaultValue = "test-data") String dataId) {
        
        log.info("⚡ 캐시 성능 비교 테스트 시작: dataId={}", dataId);
        
        Map<String, Object> results = dualCacheService.performanceComparison(dataId);
        
        // 결과 분석 추가
        Map<String, Object> analysis = new HashMap<>(results);
        analysis.put("recommendation", getPerformanceRecommendation(results));
        analysis.put("timestamp", System.currentTimeMillis());
        
        log.info("📊 캐시 성능 비교 완료: {}", results.keySet());
        
        return ResponseEntity.ok(CommonResponse.success(analysis));
    }

    /**
     * 모든 캐시 비교 (동일한 키로 모든 캐시 조회)
     */
    @Operation(summary = "모든 캐시 타입 비교", 
               description = "동일한 데이터를 모든 캐시 타입으로 조회하여 비교합니다")
    @GetMapping("/compare-all/{dataId}")
    public ResponseEntity<CommonResponse<Map<String, Object>>> compareAllCaches(
            @Parameter(description = "비교할 데이터 ID") @PathVariable String dataId) {
        
        log.info("🔍 전체 캐시 비교 테스트: dataId={}", dataId);
        
        Map<String, Object> results = new HashMap<>();
        
        for (String cacheType : cacheSelector.getAvailableCacheTypes()) {
            long startTime = System.nanoTime();
            
            try {
                CacheableData data = dualCacheService.getUserData(dataId, cacheType);
                long elapsedTime = System.nanoTime() - startTime;
                
                Map<String, Object> cacheResult = Map.of(
                    "data", data,
                    "responseTime_ns", elapsedTime,
                    "responseTime_micros", elapsedTime / 1000.0,
                    "cacheType", cacheType,
                    "status", "success"
                );
                
                results.put(cacheType, cacheResult);
                
            } catch (Exception e) {
                log.error("❌ {} 캐시 조회 실패: {}", cacheType, e.getMessage());
                
                results.put(cacheType, Map.of(
                    "status", "error",
                    "error", e.getMessage(),
                    "cacheType", cacheType
                ));
            }
        }
        
        return ResponseEntity.ok(CommonResponse.success(results));
    }

    /**
     * 캐시 통계 조회
     */
    @Operation(summary = "캐시 통계 조회", 
               description = "전체 캐시 시스템의 통계 및 특성 정보를 조회합니다")
    @GetMapping("/statistics")
    public ResponseEntity<CommonResponse<Map<String, Object>>> getCacheStatistics() {
        
        Map<String, Object> statistics = dualCacheService.getCacheStatistics();
        
        return ResponseEntity.ok(CommonResponse.success(statistics));
    }

    /**
     * 사용 가능한 캐시 타입 조회
     */
    @Operation(summary = "사용 가능한 캐시 타입", 
               description = "현재 시스템에서 사용 가능한 모든 캐시 타입을 조회합니다")
    @GetMapping("/cache-types")
    public ResponseEntity<CommonResponse<Map<String, Object>>> getAvailableCacheTypes() {
        
        Map<String, Object> cacheInfo = new HashMap<>();
        
        // 사용 가능한 캐시 타입
        cacheInfo.put("availableTypes", cacheSelector.getAvailableCacheTypes());
        
        // 각 캐시 타입별 특성
        Map<String, String> characteristics = new HashMap<>();
        for (String cacheType : cacheSelector.getAvailableCacheTypes()) {
            characteristics.put(cacheType, cacheSelector.getCacheCharacteristics(cacheType));
        }
        cacheInfo.put("characteristics", characteristics);
        
        // 성능 가이드
        cacheInfo.put("performanceComparison", performanceGuide.getPerformanceComparison());
        cacheInfo.put("useCaseRecommendation", performanceGuide.getUseCaseRecommendation());
        
        return ResponseEntity.ok(CommonResponse.success(cacheInfo));
    }

    /**
     * 모든 캐시 클리어
     */
    @Operation(summary = "모든 캐시 클리어", 
               description = "모든 캐시 타입의 데이터를 삭제하고 통계를 초기화합니다")
    @DeleteMapping("/clear-all")
    public ResponseEntity<CommonResponse<String>> clearAllCaches() {
        
        log.info("🗑️ 전체 캐시 클리어 요청");
        
        dualCacheService.clearAllCaches();
        
        return ResponseEntity.ok(CommonResponse.success("모든 캐시가 성공적으로 클리어되었습니다."));
    }

    /**
     * 캐시 워밍업
     */
    @Operation(summary = "캐시 워밍업", 
               description = "지정된 캐시 타입에 테스트 데이터를 미리 로딩합니다")
    @PostMapping("/warmup")
    public ResponseEntity<CommonResponse<Map<String, Object>>> warmupCache(
            @Parameter(description = "워밍업할 캐시 타입") 
            @RequestParam(defaultValue = "all") String cacheType,
            @Parameter(description = "워밍업 데이터 개수") 
            @RequestParam(defaultValue = "100") int dataCount) {
        
        log.info("🔥 캐시 워밍업 시작: cacheType={}, dataCount={}", cacheType, dataCount);
        
        Map<String, Object> results = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        if ("all".equals(cacheType)) {
            // 모든 캐시 타입 워밍업
            for (String type : cacheSelector.getAvailableCacheTypes()) {
                int loaded = warmupCacheType(type, dataCount);
                results.put(type + "_loaded", loaded);
            }
        } else {
            // 특정 캐시 타입 워밍업
            int loaded = warmupCacheType(cacheType, dataCount);
            results.put("loaded", loaded);
            results.put("cacheType", cacheType);
        }
        
        long elapsed = System.currentTimeMillis() - startTime;
        results.put("elapsedTime_ms", elapsed);
        
        log.info("✅ 캐시 워밍업 완료: {}ms 소요", elapsed);
        
        return ResponseEntity.ok(CommonResponse.success(results));
    }

    /**
     * 특정 캐시 타입 워밍업
     */
    private int warmupCacheType(String cacheType, int dataCount) {
        int loadedCount = 0;
        
        try {
            for (int i = 1; i <= dataCount; i++) {
                String userId = "warmup-user-" + i;
                String productId = "warmup-product-" + i;
                
                // 사용자 데이터 로딩
                dualCacheService.getUserData(userId, cacheType);
                
                // 상품 데이터 로딩 (절반만)
                if (i <= dataCount / 2) {
                    dualCacheService.getProductData(productId, cacheType);
                }
                
                loadedCount++;
            }
        } catch (Exception e) {
            log.error("❌ {} 캐시 워밍업 중 오류: {}", cacheType, e.getMessage());
        }
        
        return loadedCount;
    }

    /**
     * 성능 테스트 결과 분석 및 추천
     */
    private String getPerformanceRecommendation(Map<String, Object> results) {
        // 가장 빠른 캐시 찾기
        String fastestCache = null;
        double bestTime = Double.MAX_VALUE;
        
        for (String key : results.keySet()) {
            if (key.endsWith("_avg_time_ns")) {
                String cacheType = key.replace("_avg_time_ns", "");
                Double time = (Double) results.get(key);
                
                if (time != null && time < bestTime) {
                    bestTime = time;
                    fastestCache = cacheType;
                }
            }
        }
        
        if (fastestCache != null) {
            return String.format(
                "🏆 최고 성능: %s (%.2f μs)\n\n%s",
                fastestCache,
                bestTime / 1000.0,
                cacheSelector.getCacheCharacteristics(fastestCache)
            );
        }
        
        return "성능 분석 결과를 확인할 수 없습니다.";
    }
}