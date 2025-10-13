package com.ocean.scdemo.cache.config;

import lombok.extern.slf4j.Slf4j;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.core.config.DefaultConfiguration;
import org.ehcache.jsr107.EhcacheCachingProvider;
import org.springframework.cache.CacheManager;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.cache.Caching;
import java.time.Duration;

/**
 * EhCache 설정 클래스
 * 
 * 핵심 특징:
 * - 디스크 기반 영구 저장소 지원
 * - 오프힙 메모리 활용으로 GC 부담 감소
 * - 계층적 스토리지 (Heap -> Off-Heap -> Disk)
 * - 애플리케이션 재시작 후 데이터 복구 가능
 * - 대용량 데이터 캐싱에 적합
 */
@Slf4j
@Configuration
public class EhCacheConfig {

    /**
     * EhCache 캐시 매니저 - XML 기반 설정
     * 임시로 비활성화 - 락 문제 해결 후 활성화 예정
     */
    // @Bean("ehCacheManager")
    public CacheManager ehCacheManager_disabled() {
        try {
            // XML 설정 파일을 사용한 EhCache 매니저 생성
            ClassPathResource configLocation = new ClassPathResource("ehcache.xml");
            
            javax.cache.CacheManager cacheManager = Caching.getCachingProvider(
                EhcacheCachingProvider.class.getName()
            ).getCacheManager(
                configLocation.getURI(),
                Thread.currentThread().getContextClassLoader()
            );
            
            JCacheCacheManager ehCacheManager = new JCacheCacheManager(cacheManager);
            
            log.info("✅ EhCache 매니저 초기화 완료 - XML 설정 기반");
            log.info("📁 EhCache 저장소 위치: /tmp/ehcache-data");
            log.info("💾 지원 캐시: users, products, hotData, sessions, config, metrics");
            
            return ehCacheManager;
            
        } catch (Exception e) {
            log.error("❌ EhCache 초기화 실패", e);
            throw new RuntimeException("EhCache 설정 오류", e);
        }
    }

    /**
     * 프로그래밍 방식 EhCache 매니저 (백업용 - 힙 메모리만 사용)
     */
    @Bean("ehCacheManagerProgrammatic")
    public CacheManager ehCacheManagerProgrammatic() {
        try {
            // 프로그래밍 방식으로 EhCache 설정 (힙 메모리만 사용)
            org.ehcache.CacheManager ehCacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                // 사용자 캐시 설정
                .withCache("ehcache-users-prog",
                    CacheConfigurationBuilder.newCacheConfigurationBuilder(
                        String.class, Object.class,
                        ResourcePoolsBuilder.newResourcePoolsBuilder()
                            .heap(5000, EntryUnit.ENTRIES)      // 힙에 5000개 엔트리
                    ).withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofMinutes(30)))
                )
                // 상품 캐시 설정
                .withCache("ehcache-products-prog",
                    CacheConfigurationBuilder.newCacheConfigurationBuilder(
                        String.class, Object.class,
                        ResourcePoolsBuilder.newResourcePoolsBuilder()
                            .heap(10000, EntryUnit.ENTRIES)     // 힙에 10000개 엔트리
                    ).withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofHours(2)))
                )
                // 핫 데이터 캐시 설정
                .withCache("ehcache-hotData-prog",
                    CacheConfigurationBuilder.newCacheConfigurationBuilder(
                        String.class, Object.class,
                        ResourcePoolsBuilder.newResourcePoolsBuilder()
                            .heap(2000, EntryUnit.ENTRIES)      // 힙에 2000개 엔트리
                    ).withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofMinutes(15)))
                )
                .build();

            ehCacheManager.init();

            // Spring의 JCacheCacheManager로 래핑
            javax.cache.CacheManager jcacheManager = Caching.getCachingProvider().getCacheManager();
            JCacheCacheManager springCacheManager = new JCacheCacheManager(jcacheManager);

            log.info("✅ EhCache 프로그래밍 매니저 초기화 완료 (Heap Memory Only)");
            log.info("💾 지원 캐시: ehcache-users-prog, ehcache-products-prog, ehcache-hotData-prog");

            return springCacheManager;

        } catch (Exception e) {
            log.error("❌ EhCache 프로그래밍 설정 초기화 실패", e);
            throw new RuntimeException("EhCache 프로그래밍 설정 오류", e);
        }
    }

    /**
     * EhCache 통계 및 정보 Bean
     */
    @Bean
    public EhCacheInfo ehCacheInfo() {
        return new EhCacheInfo();
    }

    /**
     * EhCache 정보 제공 클래스
     */
    public static class EhCacheInfo {
        
        public String getStorageInfo() {
            return """
                EhCache 계층적 스토리지:
                ├─ L1: Heap Memory (빠른 액세스)
                ├─ L2: Off-Heap Memory (GC 부담 감소)
                └─ L3: Disk Storage (영구 저장)
                
                특징:
                • 애플리케이션 재시작 후 데이터 복구
                • 대용량 데이터 캐싱 지원
                • 메모리 압박 시 자동 디스크 이동
                • TTL/TTI 기반 만료 정책
                """;
        }
        
        public String getCacheNames() {
            return """
                EhCache 캐시 목록:
                ├─ ehcache-users: 사용자 데이터 (30분 TTL)
                ├─ ehcache-products: 상품 데이터 (2시간 TTL)  
                ├─ ehcache-hotData: 핫 데이터 (15분 TTL)
                ├─ ehcache-sessions: 세션 데이터 (1시간 TTL)
                ├─ ehcache-config: 설정 데이터 (24시간 TTL)
                └─ ehcache-metrics: 메트릭스 데이터 (5분 TTL)
                """;
        }
        
        public String getUsageGuideline() {
            return """
                EhCache 사용 가이드라인:
                
                적합한 사용 사례:
                ├─ 대용량 데이터 캐싱
                ├─ 영구 저장이 필요한 캐시
                ├─ 메모리 사용량 최적화가 중요한 경우
                ├─ 애플리케이션 재시작 후 캐시 유지
                └─ 복잡한 만료 정책이 필요한 경우
                
                주의사항:
                • 디스크 I/O로 인한 성능 영향 고려
                • 직렬화/역직렬화 오버헤드 존재
                • Caffeine 대비 상대적으로 느린 액세스
                """;
        }
    }
}