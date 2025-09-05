package com.ocean.scdemo.cache.controller;

import com.ocean.scdemo.cache.service.CacheMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 캐시 성능 모니터링 컨트롤러
 * 캐시 통계 및 성능 분석 정보 제공
 */
@Tag(name = "Cache Metrics", description = "캐시 성능 모니터링 및 통계 API")
@RestController
@RequestMapping("/api/cache/metrics")
@RequiredArgsConstructor
public class CacheMetricsController {

    private final CacheMetricsService metricsService;

    @Operation(summary = "전체 캐시 성능 리포트", description = "Caffeine과 Redis 캐시의 종합 성능 통계")
    @GetMapping("/report")
    public ResponseEntity<CacheMetricsService.CachePerformanceReport> getPerformanceReport() {
        return ResponseEntity.ok(metricsService.getAllCacheMetrics());
    }

    @Operation(summary = "Caffeine 캐시 통계", description = "L1 캐시 (Caffeine)의 상세 성능 통계")
    @GetMapping("/caffeine")
    public ResponseEntity<Map<String, CacheMetricsService.CaffeineMetrics>> getCaffeineMetrics() {
        return ResponseEntity.ok(metricsService.getCaffeineMetrics());
    }

    @Operation(summary = "Redis 캐시 통계", description = "L2 캐시 (Redis)의 상세 성능 통계")
    @GetMapping("/redis")
    public ResponseEntity<CacheMetricsService.RedisMetrics> getRedisMetrics() {
        return ResponseEntity.ok(metricsService.getRedisMetrics());
    }

    @Operation(summary = "캐시별 상세 분석", description = "특정 캐시의 성능 분석 및 최적화 추천")
    @GetMapping("/analysis/{cacheName}")
    public ResponseEntity<CacheMetricsService.CacheDetailAnalysis> analyzeCachePerformance(
            @Parameter(description = "분석할 캐시 이름") @PathVariable String cacheName) {
        return ResponseEntity.ok(metricsService.analyzeCachePerformance(cacheName));
    }

    @Operation(summary = "성능 알람 체크", description = "캐시 성능 이슈 확인 및 알람")
    @PostMapping("/alerts/check")
    public ResponseEntity<String> checkPerformanceAlerts() {
        metricsService.checkPerformanceAlerts();
        return ResponseEntity.ok("성능 알람 체크가 완료되었습니다. 로그를 확인하세요.");
    }

    @Operation(summary = "모니터링 시작", description = "실시간 캐시 성능 모니터링 시작")
    @PostMapping("/monitoring/start")
    public ResponseEntity<String> startMonitoring() {
        metricsService.startCacheMonitoring();
        return ResponseEntity.ok("실시간 캐시 모니터링이 시작되었습니다.");
    }
}