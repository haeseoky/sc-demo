package com.ocean.scdemo.cache.controller;

import com.ocean.scdemo.cache.service.HighPerformanceCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 캐시 성능 테스트 컨트롤러
 * 다단계 캐시 시스템의 성능을 측정하고 테스트하는 API
 */
@Tag(name = "Cache Performance", description = "다단계 캐시 성능 테스트 API")
@Slf4j
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class CachePerformanceController {

    private final HighPerformanceCacheService cacheService;

    @Operation(summary = "사용자 데이터 조회", description = "L1(Caffeine) -> L2(Redis) -> DB 순서로 조회")
    @GetMapping("/users/{userId}")
    public ResponseEntity<HighPerformanceCacheService.UserData> getUser(
            @Parameter(description = "사용자 ID") @PathVariable String userId) {
        
        long startTime = System.currentTimeMillis();
        HighPerformanceCacheService.UserData userData = cacheService.getUserData(userId);
        long endTime = System.currentTimeMillis();
        
        log.info("사용자 조회 완료 - ID: {}, 응답시간: {}ms", userId, endTime - startTime);
        return ResponseEntity.ok(userData);
    }

    @Operation(summary = "상품 데이터 조회", description = "멀티레벨 캐시를 통한 상품 정보 조회")
    @GetMapping("/products/{productId}")
    public ResponseEntity<HighPerformanceCacheService.ProductData> getProduct(
            @Parameter(description = "상품 ID") @PathVariable String productId) {
        
        long startTime = System.currentTimeMillis();
        HighPerformanceCacheService.ProductData productData = cacheService.getProductData(productId);
        long endTime = System.currentTimeMillis();
        
        log.info("상품 조회 완료 - ID: {}, 응답시간: {}ms", productId, endTime - startTime);
        return ResponseEntity.ok(productData);
    }

    @Operation(summary = "핫 데이터 조회", description = "실시간성이 중요한 데이터 조회 (짧은 TTL)")
    @GetMapping("/hotdata/{dataKey}")
    public ResponseEntity<HighPerformanceCacheService.HotData> getHotData(
            @Parameter(description = "데이터 키") @PathVariable String dataKey) {
        
        long startTime = System.currentTimeMillis();
        HighPerformanceCacheService.HotData hotData = cacheService.getHotData(dataKey);
        long endTime = System.currentTimeMillis();
        
        log.info("핫데이터 조회 완료 - Key: {}, 응답시간: {}ms", dataKey, endTime - startTime);
        return ResponseEntity.ok(hotData);
    }

    @Operation(summary = "사용자 데이터 업데이트", description = "사용자 정보 수정 및 캐시 갱신")
    @PutMapping("/users")
    public ResponseEntity<HighPerformanceCacheService.UserData> updateUser(
            @RequestBody HighPerformanceCacheService.UserData userData) {
        
        long startTime = System.currentTimeMillis();
        HighPerformanceCacheService.UserData updated = cacheService.updateUserData(userData);
        long endTime = System.currentTimeMillis();
        
        log.info("사용자 업데이트 완료 - ID: {}, 응답시간: {}ms", userData.getId(), endTime - startTime);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "사용자 데이터 삭제", description = "사용자 정보 삭제 및 캐시 제거")
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<String> deleteUser(
            @Parameter(description = "사용자 ID") @PathVariable String userId) {
        
        cacheService.deleteUserData(userId);
        return ResponseEntity.ok("사용자 삭제 완료: " + userId);
    }

    @Operation(summary = "모든 사용자 캐시 제거", description = "전체 사용자 캐시 클리어")
    @DeleteMapping("/users/cache/clear")
    public ResponseEntity<String> clearUserCache() {
        cacheService.clearAllUserCache();
        return ResponseEntity.ok("모든 사용자 캐시가 제거되었습니다.");
    }

    @Operation(summary = "캐시 예열", description = "자주 사용되는 데이터를 미리 캐시에 로드")
    @PostMapping("/warmup")
    public ResponseEntity<String> warmupCache() {
        CompletableFuture<Void> warmupFuture = cacheService.warmupCache();
        
        warmupFuture.thenRun(() -> log.info("캐시 예열이 백그라운드에서 완료되었습니다."));
        
        return ResponseEntity.ok("캐시 예열이 백그라운드에서 시작되었습니다.");
    }

    @Operation(summary = "성능 테스트", description = "대량 요청을 통한 캐시 성능 측정")
    @PostMapping("/performance-test")
    public ResponseEntity<PerformanceTestResult> performanceTest(
            @Parameter(description = "테스트 횟수") @RequestParam(defaultValue = "100") int iterations,
            @Parameter(description = "동시 사용자 수") @RequestParam(defaultValue = "10") int concurrency) {
        
        long totalStartTime = System.currentTimeMillis();
        
        List<CompletableFuture<Long>> futures = new java.util.ArrayList<>();
        
        for (int i = 0; i < concurrency; i++) {
            final int threadNum = i;
            CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
                long threadTotal = 0;
                for (int j = 0; j < iterations / concurrency; j++) {
                    long start = System.currentTimeMillis();
                    cacheService.getUserData("user" + (threadNum * 100 + j));
                    threadTotal += (System.currentTimeMillis() - start);
                }
                return threadTotal;
            });
            futures.add(future);
        }
        
        // 모든 스레드 완료 대기
        List<Long> results = futures.stream()
            .map(CompletableFuture::join)
            .toList();
        
        long totalEndTime = System.currentTimeMillis();
        long totalTime = totalEndTime - totalStartTime;
        long totalResponseTime = results.stream().mapToLong(Long::longValue).sum();
        
        PerformanceTestResult result = PerformanceTestResult.builder()
            .totalRequests(iterations)
            .concurrency(concurrency)
            .totalTimeMs(totalTime)
            .averageResponseTimeMs(totalResponseTime / (double) iterations)
            .requestsPerSecond(iterations * 1000.0 / totalTime)
            .build();
        
        log.info("성능 테스트 완료: {}", result);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "배치 사용자 조회", description = "여러 사용자를 한 번에 조회하는 성능 테스트")
    @PostMapping("/users/batch")
    public ResponseEntity<Map<String, HighPerformanceCacheService.UserData>> getBatchUsers(
            @RequestBody List<String> userIds) {
        
        long startTime = System.currentTimeMillis();
        
        Map<String, HighPerformanceCacheService.UserData> results = cacheService.getBatch(
            userIds,
            "users",
            missedKeys -> {
                // 캐시 미스된 키들을 DB에서 조회하는 로직
                return missedKeys.stream()
                    .collect(java.util.stream.Collectors.toMap(
                        key -> key,
                        key -> HighPerformanceCacheService.UserData.builder()
                            .id(key)
                            .name("배치사용자" + key)
                            .email(key + "@batch.com")
                            .score(Math.random() * 1000)
                            .build()
                    ));
            }
        );
        
        long endTime = System.currentTimeMillis();
        log.info("배치 사용자 조회 완료 - 요청수: {}, 응답시간: {}ms", userIds.size(), endTime - startTime);
        
        return ResponseEntity.ok(results);
    }

    @lombok.Builder
    @lombok.Data
    public static class PerformanceTestResult {
        private int totalRequests;
        private int concurrency;
        private long totalTimeMs;
        private double averageResponseTimeMs;
        private double requestsPerSecond;
    }
}