package com.ocean.scdemo.redispubsub.monitoring;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 지연시간 추적 및 통계 계산 클래스
 * 
 * 기능:
 * - 실시간 지연시간 추적
 * - 백분위수 계산 (P50, P95, P99)
 * - 이동 평균 계산
 * - 히스토그램 분포
 * - 임계치 위반 추적
 */
@Slf4j
public class LatencyTracker {
    
    // 기본 통계
    private final LongAdder totalSamples = new LongAdder();
    private final LongAdder totalLatency = new LongAdder();
    private final AtomicLong minLatency = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxLatency = new AtomicLong(0);
    
    // 히스토그램 버킷 (마이크로초 단위)
    private final LongAdder[] histogramBuckets;
    private final long[] bucketBounds = {
        1000L,      // 1ms
        5000L,      // 5ms
        10000L,     // 10ms
        25000L,     // 25ms
        50000L,     // 50ms
        100000L,    // 100ms
        250000L,    // 250ms
        500000L,    // 500ms
        1000000L,   // 1s
        5000000L,   // 5s
        Long.MAX_VALUE
    };
    
    // 이동 평균을 위한 원형 버퍼
    private final long[] recentSamples;
    private int currentIndex = 0;
    private boolean bufferFull = false;
    private final int bufferSize = 1000;
    
    // 임계치 추적
    private final AtomicLong thresholdViolations = new AtomicLong(0);
    private volatile long alertThreshold = 5000L; // 5초 기본값
    
    // 통계 캐시
    private volatile LatencyStats cachedStats;
    private volatile LocalDateTime lastStatsUpdate;
    private static final int CACHE_VALIDITY_SECONDS = 10;
    
    public LatencyTracker() {
        this.histogramBuckets = new LongAdder[bucketBounds.length];
        for (int i = 0; i < histogramBuckets.length; i++) {
            histogramBuckets[i] = new LongAdder();
        }
        this.recentSamples = new long[bufferSize];
    }
    
    /**
     * 지연시간 기록 (밀리초)
     */
    public void recordLatency(long latencyMs) {
        recordLatencyMicros(latencyMs * 1000L);
    }
    
    /**
     * 지연시간 기록 (마이크로초)
     */
    public void recordLatencyMicros(long latencyMicros) {
        // 기본 통계 업데이트
        totalSamples.increment();
        totalLatency.add(latencyMicros);
        
        // 최소/최대값 업데이트
        updateMinLatency(latencyMicros);
        updateMaxLatency(latencyMicros);
        
        // 히스토그램 업데이트
        updateHistogram(latencyMicros);
        
        // 이동 평균 버퍼 업데이트
        updateRecentSamples(latencyMicros);
        
        // 임계치 위반 체크
        if (latencyMicros > alertThreshold * 1000L) {
            thresholdViolations.incrementAndGet();
        }
        
        // 캐시 무효화
        invalidateCache();
    }
    
    /**
     * 현재 통계 조회
     */
    public LatencyStats getStats() {
        if (isStatsCacheValid()) {
            return cachedStats;
        }
        
        return calculateAndCacheStats();
    }
    
    /**
     * 임계치 설정
     */
    public void setAlertThreshold(long thresholdMs) {
        this.alertThreshold = thresholdMs;
    }
    
    /**
     * 통계 리셋
     */
    public synchronized void reset() {
        totalSamples.reset();
        totalLatency.reset();
        minLatency.set(Long.MAX_VALUE);
        maxLatency.set(0);
        thresholdViolations.set(0);
        
        for (LongAdder bucket : histogramBuckets) {
            bucket.reset();
        }
        
        // 버퍼 초기화
        currentIndex = 0;
        bufferFull = false;
        
        invalidateCache();
    }
    
    // === 내부 구현 메서드들 ===
    
    private void updateMinLatency(long latency) {
        long currentMin = minLatency.get();
        while (latency < currentMin && !minLatency.compareAndSet(currentMin, latency)) {
            currentMin = minLatency.get();
        }
    }
    
    private void updateMaxLatency(long latency) {
        long currentMax = maxLatency.get();
        while (latency > currentMax && !maxLatency.compareAndSet(currentMax, latency)) {
            currentMax = maxLatency.get();
        }
    }
    
    private void updateHistogram(long latencyMicros) {
        for (int i = 0; i < bucketBounds.length; i++) {
            if (latencyMicros <= bucketBounds[i]) {
                histogramBuckets[i].increment();
                break;
            }
        }
    }
    
    private synchronized void updateRecentSamples(long latency) {
        recentSamples[currentIndex] = latency;
        currentIndex = (currentIndex + 1) % bufferSize;
        
        if (currentIndex == 0) {
            bufferFull = true;
        }
    }
    
    private boolean isStatsCacheValid() {
        if (lastStatsUpdate == null || cachedStats == null) {
            return false;
        }
        
        LocalDateTime expiry = lastStatsUpdate.plusSeconds(CACHE_VALIDITY_SECONDS);
        return LocalDateTime.now().isBefore(expiry);
    }
    
    private void invalidateCache() {
        lastStatsUpdate = null;
        cachedStats = null;
    }
    
    private LatencyStats calculateAndCacheStats() {
        long samples = totalSamples.sum();
        if (samples == 0) {
            return LatencyStats.empty();
        }
        
        // 기본 통계
        long total = totalLatency.sum();
        double average = (double) total / samples / 1000.0; // 밀리초로 변환
        long min = minLatency.get() == Long.MAX_VALUE ? 0 : minLatency.get() / 1000L;
        long max = maxLatency.get() / 1000L;
        
        // 백분위수 계산
        PercentileCalculator calculator = new PercentileCalculator();
        double p50 = calculator.calculateP50();
        double p95 = calculator.calculateP95();
        double p99 = calculator.calculateP99();
        
        // 이동 평균
        double recentAverage = calculateRecentAverage();
        
        // 히스토그램 데이터
        HistogramData histogram = buildHistogramData();
        
        // 통계 객체 생성
        LatencyStats stats = LatencyStats.builder()
                .sampleCount(samples)
                .average(average)
                .minimum(min)
                .maximum(max)
                .p50(p50)
                .p95(p95)
                .p99(p99)
                .recentAverage(recentAverage)
                .thresholdViolations(thresholdViolations.get())
                .alertThreshold(alertThreshold)
                .histogram(histogram)
                .lastUpdate(LocalDateTime.now())
                .build();
        
        // 캐시 업데이트
        cachedStats = stats;
        lastStatsUpdate = LocalDateTime.now();
        
        return stats;
    }
    
    private double calculateRecentAverage() {
        if (!bufferFull && currentIndex == 0) {
            return 0.0;
        }
        
        int count = bufferFull ? bufferSize : currentIndex;
        long sum = 0;
        
        for (int i = 0; i < count; i++) {
            sum += recentSamples[i];
        }
        
        return (double) sum / count / 1000.0; // 밀리초로 변환
    }
    
    private HistogramData buildHistogramData() {
        HistogramData.HistogramDataBuilder builder = HistogramData.builder();
        
        builder.bucket1ms(histogramBuckets[0].sum());
        builder.bucket5ms(histogramBuckets[1].sum());
        builder.bucket10ms(histogramBuckets[2].sum());
        builder.bucket25ms(histogramBuckets[3].sum());
        builder.bucket50ms(histogramBuckets[4].sum());
        builder.bucket100ms(histogramBuckets[5].sum());
        builder.bucket250ms(histogramBuckets[6].sum());
        builder.bucket500ms(histogramBuckets[7].sum());
        builder.bucket1s(histogramBuckets[8].sum());
        builder.bucket5s(histogramBuckets[9].sum());
        builder.bucketInf(histogramBuckets[10].sum());
        
        return builder.build();
    }
    
    // === 백분위수 계산 클래스 ===
    
    private class PercentileCalculator {
        
        public double calculateP50() {
            return calculatePercentile(0.5);
        }
        
        public double calculateP95() {
            return calculatePercentile(0.95);
        }
        
        public double calculateP99() {
            return calculatePercentile(0.99);
        }
        
        private double calculatePercentile(double percentile) {
            long totalSamples = getTotalSamples();
            if (totalSamples == 0) return 0.0;
            
            long targetSample = (long) (totalSamples * percentile);
            long currentCount = 0;
            
            // 히스토그램을 이용한 근사치 계산
            for (int i = 0; i < histogramBuckets.length; i++) {
                long bucketCount = histogramBuckets[i].sum();
                currentCount += bucketCount;
                
                if (currentCount >= targetSample) {
                    // 버킷의 중점값을 반환 (마이크로초를 밀리초로 변환)
                    long bucketMidpoint = i == 0 ? bucketBounds[i] / 2 : 
                                        (bucketBounds[i-1] + bucketBounds[i]) / 2;
                    return bucketMidpoint / 1000.0;
                }
            }
            
            return maxLatency.get() / 1000.0; // 최대값 반환
        }
        
        private long getTotalSamples() {
            return totalSamples.sum();
        }
    }
}

/**
 * 지연시간 통계 데이터 클래스
 */
@lombok.Builder
@lombok.Data
class LatencyStats {
    private final long sampleCount;
    private final double average;        // 평균 (ms)
    private final long minimum;          // 최소값 (ms)
    private final long maximum;          // 최대값 (ms)
    private final double p50;            // 50 백분위수 (ms)
    private final double p95;            // 95 백분위수 (ms)
    private final double p99;            // 99 백분위수 (ms)
    private final double recentAverage;  // 최근 이동 평균 (ms)
    private final long thresholdViolations; // 임계치 위반 수
    private final long alertThreshold;   // 알림 임계치 (ms)
    private final HistogramData histogram; // 히스토그램 데이터
    private final LocalDateTime lastUpdate;
    
    public static LatencyStats empty() {
        return LatencyStats.builder()
                .sampleCount(0)
                .average(0.0)
                .minimum(0)
                .maximum(0)
                .p50(0.0)
                .p95(0.0)
                .p99(0.0)
                .recentAverage(0.0)
                .thresholdViolations(0)
                .alertThreshold(5000)
                .histogram(HistogramData.empty())
                .lastUpdate(LocalDateTime.now())
                .build();
    }
    
    /**
     * 성능 상태 평가
     */
    public PerformanceStatus getPerformanceStatus() {
        if (p95 < 100) return PerformanceStatus.EXCELLENT;
        if (p95 < 500) return PerformanceStatus.GOOD;
        if (p95 < 1000) return PerformanceStatus.ACCEPTABLE;
        if (p95 < 5000) return PerformanceStatus.POOR;
        return PerformanceStatus.CRITICAL;
    }
    
    public enum PerformanceStatus {
        EXCELLENT("훌륭함"),
        GOOD("양호"),
        ACCEPTABLE("허용 가능"),
        POOR("불량"),
        CRITICAL("심각");
        
        private final String description;
        
        PerformanceStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}

/**
 * 히스토그램 데이터 클래스
 */
@lombok.Builder
@lombok.Data
class HistogramData {
    private final long bucket1ms;
    private final long bucket5ms;
    private final long bucket10ms;
    private final long bucket25ms;
    private final long bucket50ms;
    private final long bucket100ms;
    private final long bucket250ms;
    private final long bucket500ms;
    private final long bucket1s;
    private final long bucket5s;
    private final long bucketInf;
    
    public static HistogramData empty() {
        return HistogramData.builder()
                .bucket1ms(0)
                .bucket5ms(0)
                .bucket10ms(0)
                .bucket25ms(0)
                .bucket50ms(0)
                .bucket100ms(0)
                .bucket250ms(0)
                .bucket500ms(0)
                .bucket1s(0)
                .bucket5s(0)
                .bucketInf(0)
                .build();
    }
    
    /**
     * 히스토그램 데이터를 배열로 반환
     */
    public long[] toArray() {
        return new long[] {
            bucket1ms, bucket5ms, bucket10ms, bucket25ms, bucket50ms,
            bucket100ms, bucket250ms, bucket500ms, bucket1s, bucket5s, bucketInf
        };
    }
    
    /**
     * 라벨 배열
     */
    public static String[] getLabels() {
        return new String[] {
            "≤1ms", "≤5ms", "≤10ms", "≤25ms", "≤50ms",
            "≤100ms", "≤250ms", "≤500ms", "≤1s", "≤5s", ">5s"
        };
    }
}