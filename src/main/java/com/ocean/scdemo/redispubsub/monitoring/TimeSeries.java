package com.ocean.scdemo.redispubsub.monitoring;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

/**
 * 시계열 데이터 저장 및 관리 클래스
 * 
 * 기능:
 * - 시간순 데이터 포인트 저장
 * - 시간 범위별 데이터 조회
 * - 통계 계산 (평균, 최대, 최소)
 * - 데이터 집계 및 샘플링
 * - 오래된 데이터 자동 정리
 */
@Slf4j
@Data
public class TimeSeries {
    
    private final String metricName;
    private final MetricsCollectorService.MetricType type;
    
    // 시간순으로 정렬된 데이터 포인트 저장
    private final NavigableMap<LocalDateTime, DataPoint> dataPoints = new ConcurrentSkipListMap<>();
    
    // 캐시된 통계 (성능 최적화용)
    private volatile StatisticsCache statisticsCache;
    private LocalDateTime lastCacheUpdate;
    
    // 데이터 보존 정책
    private static final int MAX_DATA_POINTS = 10000;
    private static final int CACHE_VALIDITY_MINUTES = 5;
    
    public TimeSeries(String metricName, MetricsCollectorService.MetricType type) {
        this.metricName = metricName;
        this.type = type;
        this.statisticsCache = new StatisticsCache();
        this.lastCacheUpdate = LocalDateTime.now();
    }
    
    /**
     * 데이터 포인트 추가
     */
    public synchronized void addDataPoint(double value, LocalDateTime timestamp) {
        DataPoint point = new DataPoint(value, timestamp);
        dataPoints.put(timestamp, point);
        
        // 캐시 무효화
        invalidateCache();
        
        // 데이터 포인트 수 제한
        if (dataPoints.size() > MAX_DATA_POINTS) {
            removeOldestDataPoint();
        }
        
        log.trace("데이터 포인트 추가: {}={} at {}", metricName, value, timestamp);
    }
    
    /**
     * 현재 값 (가장 최근 데이터)
     */
    public double getCurrentValue() {
        if (dataPoints.isEmpty()) return 0.0;
        return dataPoints.lastEntry().getValue().getValue();
    }
    
    /**
     * 시간 범위별 데이터 조회
     */
    public TimeSeries filterByTimeRange(LocalDateTime from, LocalDateTime to) {
        TimeSeries filtered = new TimeSeries(this.metricName + "_filtered", this.type);
        
        NavigableMap<LocalDateTime, DataPoint> rangeData = dataPoints.subMap(from, true, to, true);
        rangeData.forEach((timestamp, dataPoint) -> {
            filtered.dataPoints.put(timestamp, dataPoint);
        });
        
        return filtered;
    }
    
    /**
     * 최근 N개 데이터 포인트 조회
     */
    public List<DataPoint> getRecentDataPoints(int count) {
        return dataPoints.values()
                .stream()
                .skip(Math.max(0, dataPoints.size() - count))
                .collect(Collectors.toList());
    }
    
    /**
     * 통계 계산
     */
    public Statistics getStatistics() {
        if (isStatisticsCacheValid()) {
            return statisticsCache.getStatistics();
        }
        
        return calculateAndCacheStatistics();
    }
    
    /**
     * 평균값 계산
     */
    public double calculateAverage() {
        if (dataPoints.isEmpty()) return 0.0;
        
        return dataPoints.values().stream()
                .mapToDouble(DataPoint::getValue)
                .average()
                .orElse(0.0);
    }
    
    /**
     * 최대값 조회
     */
    public double getMaximum() {
        return dataPoints.values().stream()
                .mapToDouble(DataPoint::getValue)
                .max()
                .orElse(0.0);
    }
    
    /**
     * 최소값 조회
     */
    public double getMinimum() {
        return dataPoints.values().stream()
                .mapToDouble(DataPoint::getValue)
                .min()
                .orElse(0.0);
    }
    
    /**
     * 합계 계산
     */
    public double getSum() {
        return dataPoints.values().stream()
                .mapToDouble(DataPoint::getValue)
                .sum();
    }
    
    /**
     * 데이터 포인트 수 조회
     */
    public int getDataPointCount() {
        return dataPoints.size();
    }
    
    /**
     * 데이터 샘플링 (다운샘플링)
     */
    public TimeSeries downsample(int targetPoints) {
        if (dataPoints.size() <= targetPoints) {
            return this; // 이미 목표 크기 이하
        }
        
        TimeSeries sampled = new TimeSeries(this.metricName + "_sampled", this.type);
        
        List<DataPoint> allPoints = new ArrayList<>(dataPoints.values());
        int step = allPoints.size() / targetPoints;
        
        for (int i = 0; i < allPoints.size(); i += step) {
            DataPoint point = allPoints.get(i);
            sampled.dataPoints.put(point.getTimestamp(), point);
        }
        
        // 마지막 포인트는 항상 포함
        if (!allPoints.isEmpty()) {
            DataPoint lastPoint = allPoints.get(allPoints.size() - 1);
            sampled.dataPoints.put(lastPoint.getTimestamp(), lastPoint);
        }
        
        return sampled;
    }
    
    /**
     * 시간 간격별 집계
     */
    public Map<LocalDateTime, AggregatedData> aggregateByInterval(int intervalMinutes) {
        Map<LocalDateTime, List<DataPoint>> buckets = new TreeMap<>();
        
        // 버킷 생성
        for (DataPoint point : dataPoints.values()) {
            LocalDateTime bucketTime = point.getTimestamp()
                    .withMinute((point.getTimestamp().getMinute() / intervalMinutes) * intervalMinutes)
                    .withSecond(0)
                    .withNano(0);
            
            buckets.computeIfAbsent(bucketTime, k -> new ArrayList<>()).add(point);
        }
        
        // 집계 계산
        Map<LocalDateTime, AggregatedData> aggregated = new TreeMap<>();
        buckets.forEach((bucketTime, points) -> {
            AggregatedData agg = new AggregatedData();
            agg.setTimestamp(bucketTime);
            agg.setCount(points.size());
            agg.setSum(points.stream().mapToDouble(DataPoint::getValue).sum());
            agg.setAverage(agg.getSum() / agg.getCount());
            agg.setMinimum(points.stream().mapToDouble(DataPoint::getValue).min().orElse(0.0));
            agg.setMaximum(points.stream().mapToDouble(DataPoint::getValue).max().orElse(0.0));
            
            aggregated.put(bucketTime, agg);
        });
        
        return aggregated;
    }
    
    /**
     * 오래된 데이터 제거
     */
    public int removeOldData(LocalDateTime cutoffTime) {
        int removedCount = 0;
        
        Iterator<Map.Entry<LocalDateTime, DataPoint>> iterator = dataPoints.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<LocalDateTime, DataPoint> entry = iterator.next();
            if (entry.getKey().isBefore(cutoffTime)) {
                iterator.remove();
                removedCount++;
            } else {
                break; // NavigableMap이므로 시간순 정렬되어 있음
            }
        }
        
        if (removedCount > 0) {
            invalidateCache();
            log.debug("오래된 데이터 제거: {} {}개 데이터 포인트", metricName, removedCount);
        }
        
        return removedCount;
    }
    
    /**
     * 데이터 압축 (중복 제거 및 노이즈 필터링)
     */
    public int compressData(double noiseThreshold) {
        if (dataPoints.size() < 3) return 0; // 최소 3개 데이터 필요
        
        List<Map.Entry<LocalDateTime, DataPoint>> entries = new ArrayList<>(dataPoints.entrySet());
        List<Map.Entry<LocalDateTime, DataPoint>> compressed = new ArrayList<>();
        
        compressed.add(entries.get(0)); // 첫 번째는 항상 유지
        
        for (int i = 1; i < entries.size() - 1; i++) {
            DataPoint prev = entries.get(i - 1).getValue();
            DataPoint curr = entries.get(i).getValue();
            DataPoint next = entries.get(i + 1).getValue();
            
            // 중요한 변화점인지 확인
            boolean significantChange = Math.abs(curr.getValue() - prev.getValue()) > noiseThreshold ||
                                      Math.abs(next.getValue() - curr.getValue()) > noiseThreshold;
            
            if (significantChange) {
                compressed.add(entries.get(i));
            }
        }
        
        compressed.add(entries.get(entries.size() - 1)); // 마지막은 항상 유지
        
        int removedCount = dataPoints.size() - compressed.size();
        
        if (removedCount > 0) {
            dataPoints.clear();
            compressed.forEach(entry -> dataPoints.put(entry.getKey(), entry.getValue()));
            invalidateCache();
            
            log.debug("데이터 압축: {} {}개 데이터 포인트 제거", metricName, removedCount);
        }
        
        return removedCount;
    }
    
    /**
     * 빈 데이터 여부 확인
     */
    public boolean isEmpty() {
        return dataPoints.isEmpty();
    }
    
    /**
     * 데이터 지우기
     */
    public void clear() {
        dataPoints.clear();
        invalidateCache();
    }
    
    // === 내부 구현 메서드들 ===
    
    private void removeOldestDataPoint() {
        if (!dataPoints.isEmpty()) {
            LocalDateTime oldest = dataPoints.firstKey();
            dataPoints.remove(oldest);
        }
    }
    
    private boolean isStatisticsCacheValid() {
        if (lastCacheUpdate == null) return false;
        
        LocalDateTime expiryTime = lastCacheUpdate.plusMinutes(CACHE_VALIDITY_MINUTES);
        return LocalDateTime.now().isBefore(expiryTime);
    }
    
    private void invalidateCache() {
        lastCacheUpdate = null;
    }
    
    private Statistics calculateAndCacheStatistics() {
        if (dataPoints.isEmpty()) {
            return Statistics.empty();
        }
        
        double[] values = dataPoints.values().stream()
                .mapToDouble(DataPoint::getValue)
                .toArray();
        
        Arrays.sort(values);
        
        Statistics stats = Statistics.builder()
                .count(values.length)
                .sum(Arrays.stream(values).sum())
                .average(Arrays.stream(values).average().orElse(0.0))
                .minimum(values[0])
                .maximum(values[values.length - 1])
                .median(calculateMedian(values))
                .percentile95(calculatePercentile(values, 0.95))
                .percentile99(calculatePercentile(values, 0.99))
                .standardDeviation(calculateStandardDeviation(values))
                .build();
        
        // 캐시 업데이트
        statisticsCache = new StatisticsCache();
        statisticsCache.setStatistics(stats);
        lastCacheUpdate = LocalDateTime.now();
        
        return stats;
    }
    
    private double calculateMedian(double[] sortedValues) {
        int n = sortedValues.length;
        if (n % 2 == 0) {
            return (sortedValues[n/2 - 1] + sortedValues[n/2]) / 2.0;
        } else {
            return sortedValues[n/2];
        }
    }
    
    private double calculatePercentile(double[] sortedValues, double percentile) {
        int n = sortedValues.length;
        int index = (int) Math.ceil(percentile * n) - 1;
        index = Math.max(0, Math.min(index, n - 1));
        return sortedValues[index];
    }
    
    private double calculateStandardDeviation(double[] values) {
        double mean = Arrays.stream(values).average().orElse(0.0);
        double variance = Arrays.stream(values)
                .map(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0.0);
        return Math.sqrt(variance);
    }
    
    // === 내부 클래스들 ===
    
    @Data
    public static class DataPoint {
        private final double value;
        private final LocalDateTime timestamp;
        
        public DataPoint(double value, LocalDateTime timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
    }
    
    @lombok.Builder
    @lombok.Data
    public static class Statistics {
        private int count;
        private double sum;
        private double average;
        private double minimum;
        private double maximum;
        private double median;
        private double percentile95;
        private double percentile99;
        private double standardDeviation;
        
        public static Statistics empty() {
            return Statistics.builder().build();
        }
    }
    
    @Data
    public static class AggregatedData {
        private LocalDateTime timestamp;
        private int count;
        private double sum;
        private double average;
        private double minimum;
        private double maximum;
    }
    
    @Data
    private static class StatisticsCache {
        private Statistics statistics;
    }
}