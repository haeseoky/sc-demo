package com.ocean.scdemo.cache.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Map;

/**
 * 듀얼 캐시 시스템 통합 설정
 * 
 * 캐시 선택 전략:
 * - Caffeine: 초고속 인메모리 캐시 (기본값)
 * - EhCache: 대용량 디스크 기반 캐시
 * - MultiLevel: Caffeine + Redis 조합
 * 
 * 설정 방법:
 * application.yml에서 cache.provider 속성으로 선택
 * - caffeine: Caffeine 캐시 사용
 * - ehcache: EhCache 사용  
 * - multilevel: 멀티레벨 캐시 사용
 */
@Slf4j
@Configuration
@EnableCaching
@RequiredArgsConstructor
public class DualCacheConfig {

    private final MultiLevelCacheConfig multiLevelCacheConfig;
    private final EhCacheConfig ehCacheConfig;

    /**
     * 기본 캐시 매니저 - Caffeine 기반
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "cache.provider", havingValue = "caffeine", matchIfMissing = true)
    public CacheManager defaultCacheManager() {
        log.info("🚀 기본 캐시 제공자: Caffeine (고성능 인메모리)");
        return multiLevelCacheConfig.caffeineCacheManager();
    }

    /**
     * EhCache 캐시 매니저 선택
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "cache.provider", havingValue = "ehcache")
    public CacheManager ehCacheCacheManager() {
        log.info("💾 기본 캐시 제공자: EhCache (대용량 디스크 기반)");
        return ehCacheConfig.ehCacheManager();
    }

    /**
     * 멀티레벨 캐시 매니저 선택
     */
    @Bean
    @Primary  
    @ConditionalOnProperty(name = "cache.provider", havingValue = "multilevel")
    public CacheManager multiLevelCacheManagerPrimary() {
        log.info("🏗️ 기본 캐시 제공자: MultiLevel (Caffeine + Redis)");
        return multiLevelCacheConfig.multiLevelCacheManager(null);
    }

    /**
     * 캐시 선택기 - 런타임에 캐시 타입 선택 가능
     */
    @Bean
    public CacheSelector cacheSelector(
            CacheManager caffeineCacheManager,
            CacheManager ehCacheManager,
            CacheManager multiLevelCacheManager) {
        
        return new CacheSelector(Map.of(
            "caffeine", caffeineCacheManager,
            "ehcache", ehCacheManager, 
            "multilevel", multiLevelCacheManager
        ));
    }

    /**
     * 캐시 선택기 클래스
     */
    public static class CacheSelector {
        
        private final Map<String, CacheManager> cacheManagers;
        
        public CacheSelector(Map<String, CacheManager> cacheManagers) {
            this.cacheManagers = cacheManagers;
        }
        
        /**
         * 캐시 타입에 따른 캐시 매니저 반환
         */
        public CacheManager getCacheManager(String cacheType) {
            CacheManager manager = cacheManagers.get(cacheType.toLowerCase());
            if (manager == null) {
                log.warn("⚠️ 알 수 없는 캐시 타입: {}. Caffeine을 기본값으로 사용합니다.", cacheType);
                return cacheManagers.get("caffeine");
            }
            return manager;
        }
        
        /**
         * 사용 가능한 캐시 타입 목록
         */
        public java.util.Set<String> getAvailableCacheTypes() {
            return cacheManagers.keySet();
        }
        
        /**
         * 캐시별 특성 정보
         */
        public String getCacheCharacteristics(String cacheType) {
            return switch (cacheType.toLowerCase()) {
                case "caffeine" -> """
                    Caffeine 캐시 특성:
                    ✅ 초고속 메모리 액세스 (나노초 단위)
                    ✅ 자동 크기 조절 및 만료 정책
                    ✅ 높은 동시성 처리 능력
                    ✅ JVM 힙 메모리 기반
                    ❌ 애플리케이션 재시작 시 데이터 손실
                    ❌ 메모리 사용량 제한
                    """;
                case "ehcache" -> """
                    EhCache 캐시 특성:
                    ✅ 대용량 데이터 저장 가능
                    ✅ 디스크 기반 영구 저장
                    ✅ 오프힙 메모리 활용 (GC 부담 감소)
                    ✅ 애플리케이션 재시작 후 데이터 복구
                    ❌ 디스크 I/O로 인한 성능 저하
                    ❌ 직렬화/역직렬화 오버헤드
                    """;
                case "multilevel" -> """
                    MultiLevel 캐시 특성:
                    ✅ L1(Caffeine) + L2(Redis) 조합
                    ✅ 로컬/글로벌 캐시 이중화
                    ✅ 확장성과 성능의 균형
                    ✅ 분산 환경 지원
                    ❌ 복잡한 설정 및 관리
                    ❌ 네트워크 지연 가능성
                    """;
                default -> "알 수 없는 캐시 타입";
            };
        }
    }

    /**
     * 캐시 성능 비교 정보 제공
     */
    @Bean
    public CachePerformanceGuide cachePerformanceGuide() {
        return new CachePerformanceGuide();
    }

    public static class CachePerformanceGuide {
        
        public String getPerformanceComparison() {
            return """
                캐시 성능 비교 가이드:
                
                📊 액세스 속도 (빠름 → 느림):
                1️⃣ Caffeine (나노초) - 메모리 직접 액세스
                2️⃣ MultiLevel L1 (나노초) - Caffeine 레이어
                3️⃣ MultiLevel L2 (밀리초) - Redis 네트워크 
                4️⃣ EhCache Heap (마이크로초) - 힙 메모리
                5️⃣ EhCache Off-Heap (마이크로초) - 오프힙 메모리
                6️⃣ EhCache Disk (밀리초) - 디스크 I/O
                
                💾 저장 용량 (작음 → 큼):
                1️⃣ Caffeine - JVM 힙 메모리 제한
                2️⃣ MultiLevel - Caffeine + Redis 조합
                3️⃣ EhCache - 힙 + 오프힙 + 디스크
                
                🔄 데이터 영속성:
                ❌ Caffeine - 휘발성 (재시작 시 손실)
                ⚠️ MultiLevel - Redis 설정에 따라
                ✅ EhCache - 디스크 영구 저장
                """;
        }
        
        public String getUseCaseRecommendation() {
            return """
                사용 사례별 캐시 선택 가이드:
                
                🔥 Caffeine 추천 상황:
                • 자주 액세스하는 소량 데이터
                • 초고속 응답이 필요한 API
                • 메모리가 충분한 단일 인스턴스
                • 개발/테스트 환경
                
                💿 EhCache 추천 상황:  
                • 대용량 데이터 캐싱
                • 메모리 사용량 최적화 필요
                • 애플리케이션 재시작 후 캐시 유지
                • 복잡한 만료 정책 필요
                
                🏗️ MultiLevel 추천 상황:
                • 분산 환경 (여러 인스턴스)
                • 로컬+글로벌 캐시 조합 필요
                • 확장성과 성능 모두 중요
                • 마이크로서비스 아키텍처
                """;
        }
    }
}