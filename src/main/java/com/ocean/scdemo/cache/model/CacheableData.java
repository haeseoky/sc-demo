package com.ocean.scdemo.cache.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 캐시 가능한 데이터 모델
 * 
 * 특징:
 * - 모든 캐시 타입 (Caffeine, EhCache, Redis)에서 호환
 * - 직렬화/역직렬화 지원
 * - 다양한 데이터 타입을 포괄하는 범용 모델
 * - 메타데이터를 통한 확장성 제공
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheableData implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 데이터 고유 식별자
     */
    private String id;

    /**
     * 데이터 이름 (사용자명, 상품명 등)
     */
    private String name;

    /**
     * 이메일 주소 (사용자 데이터의 경우)
     */
    private String email;

    /**
     * 점수 또는 평점
     */
    private Double score;

    /**
     * 생성 일시 (문자열 형태로 저장하여 직렬화 호환성 확보)
     */
    private String createdAt;

    /**
     * 메타데이터 - 확장 가능한 추가 정보
     * - 사용자: 접근 횟수, 마지막 로그인, 설정 정보
     * - 상품: 카테고리, 가격, 재고, 리뷰 수
     * - 기타: 타입별 특화 정보
     */
    private Map<String, Object> metadata;

    /**
     * 데이터 타입 구분자
     */
    private String dataType;

    /**
     * 캐시 관련 메타데이터
     */
    private CacheMeta cacheMeta;

    /**
     * 캐시 메타데이터 내부 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CacheMeta implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        /**
         * 캐시된 시점
         */
        private Long cachedAt;
        
        /**
         * 캐시 타입 (caffeine, ehcache, redis 등)
         */
        private String cacheType;
        
        /**
         * TTL (Time To Live) - 캐시 만료 시간 (초)
         */
        private Integer ttlSeconds;
        
        /**
         * 액세스 횟수
         */
        private Integer accessCount;
        
        /**
         * 마지막 액세스 시간
         */
        private Long lastAccessedAt;
        
        /**
         * 캐시 히트/미스 여부
         */
        private Boolean cacheHit;
    }

    /**
     * 편의 메서드: 사용자 데이터 생성
     */
    public static CacheableData createUserData(String userId, String name, String email, Double score) {
        return CacheableData.builder()
                .id(userId)
                .name(name)
                .email(email)
                .score(score)
                .dataType("USER")
                .build();
    }

    /**
     * 편의 메서드: 상품 데이터 생성
     */
    public static CacheableData createProductData(String productId, String name, Double rating) {
        return CacheableData.builder()
                .id(productId)
                .name(name)
                .score(rating)
                .dataType("PRODUCT")
                .build();
    }

    /**
     * 편의 메서드: 캐시 메타데이터 설정
     */
    public CacheableData withCacheMeta(String cacheType, Integer ttlSeconds) {
        this.cacheMeta = CacheMeta.builder()
                .cachedAt(System.currentTimeMillis())
                .cacheType(cacheType)
                .ttlSeconds(ttlSeconds)
                .accessCount(1)
                .lastAccessedAt(System.currentTimeMillis())
                .cacheHit(false) // 최초 생성시는 캐시 미스
                .build();
        return this;
    }

    /**
     * 편의 메서드: 액세스 횟수 증가
     */
    public void incrementAccessCount(boolean isHit) {
        if (this.cacheMeta != null) {
            this.cacheMeta.setAccessCount(this.cacheMeta.getAccessCount() + 1);
            this.cacheMeta.setLastAccessedAt(System.currentTimeMillis());
            this.cacheMeta.setCacheHit(isHit);
        }
    }

    /**
     * 편의 메서드: 데이터 유효성 검증
     */
    public boolean isValid() {
        return id != null && !id.trim().isEmpty() && name != null && !name.trim().isEmpty();
    }

    /**
     * 편의 메서드: 캐시 만료 여부 확인
     */
    public boolean isExpired() {
        if (cacheMeta == null || cacheMeta.getCachedAt() == null || cacheMeta.getTtlSeconds() == null) {
            return false; // TTL이 설정되지 않은 경우 만료되지 않음
        }
        
        long expiryTime = cacheMeta.getCachedAt() + (cacheMeta.getTtlSeconds() * 1000L);
        return System.currentTimeMillis() > expiryTime;
    }

    /**
     * 편의 메서드: 문자열 표현
     */
    @Override
    public String toString() {
        return String.format("CacheableData{id='%s', name='%s', type='%s', cacheType='%s'}", 
                            id, name, dataType, 
                            cacheMeta != null ? cacheMeta.getCacheType() : "none");
    }
}