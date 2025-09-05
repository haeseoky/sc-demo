package com.ocean.scdemo.cache.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.*;

/**
 * 멀티레벨 캐시 매니저
 * L1 (Caffeine) -> L2 (Redis) 순서로 캐시를 확인
 */
@Slf4j
@RequiredArgsConstructor
public class MultiLevelCacheManager implements CacheManager {
    
    private final CacheManager l1CacheManager;  // Caffeine (로컬)
    private final CacheManager l2CacheManager;  // Redis (글로벌)

    @Override
    public Cache getCache(String name) {
        Cache l1Cache = l1CacheManager.getCache(name);
        Cache l2Cache = l2CacheManager.getCache(name);
        
        if (l1Cache != null && l2Cache != null) {
            return new MultiLevelCache(name, l1Cache, l2Cache);
        } else if (l1Cache != null) {
            log.warn("L2 캐시를 찾을 수 없음: {}, L1 캐시만 사용", name);
            return l1Cache;
        } else if (l2Cache != null) {
            log.warn("L1 캐시를 찾을 수 없음: {}, L2 캐시만 사용", name);
            return l2Cache;
        }
        
        log.warn("캐시를 찾을 수 없음: {}", name);
        return null;
    }

    @Override
    public Collection<String> getCacheNames() {
        Set<String> names = new HashSet<>();
        names.addAll(l1CacheManager.getCacheNames());
        names.addAll(l2CacheManager.getCacheNames());
        return names;
    }

    /**
     * 멀티레벨 캐시 구현
     */
    @Slf4j
    @RequiredArgsConstructor
    public static class MultiLevelCache implements Cache {
        
        private final String name;
        private final Cache l1Cache;  // Caffeine
        private final Cache l2Cache;  // Redis

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object getNativeCache() {
            return Map.of("l1", l1Cache.getNativeCache(), "l2", l2Cache.getNativeCache());
        }

        @Override
        public ValueWrapper get(Object key) {
            // 1. L1 캐시에서 먼저 조회 (가장 빠름)
            ValueWrapper l1Value = l1Cache.get(key);
            if (l1Value != null) {
                log.debug("L1 캐시 히트: {} - {}", name, key);
                return l1Value;
            }

            // 2. L2 캐시에서 조회
            ValueWrapper l2Value = l2Cache.get(key);
            if (l2Value != null) {
                log.debug("L2 캐시 히트: {} - {}", name, key);
                // L2에서 찾은 데이터를 L1에도 저장 (캐시 워밍)
                l1Cache.put(key, l2Value.get());
                return l2Value;
            }

            log.debug("캐시 미스: {} - {}", name, key);
            return null;
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            ValueWrapper wrapper = get(key);
            return wrapper != null ? (T) wrapper.get() : null;
        }

        @Override
        public <T> T get(Object key, java.util.concurrent.Callable<T> valueLoader) {
            // 1. L1 캐시에서 먼저 조회
            ValueWrapper l1Value = l1Cache.get(key);
            if (l1Value != null) {
                log.debug("L1 캐시 히트: {} - {}", name, key);
                return (T) l1Value.get();
            }

            // 2. L2 캐시에서 조회
            ValueWrapper l2Value = l2Cache.get(key);
            if (l2Value != null) {
                log.debug("L2 캐시 히트: {} - {}", name, key);
                // L2에서 찾은 데이터를 L1에도 저장
                l1Cache.put(key, l2Value.get());
                return (T) l2Value.get();
            }

            // 3. 캐시 미스 - valueLoader를 통해 데이터 로드
            try {
                log.debug("캐시 미스, valueLoader 실행: {} - {}", name, key);
                T value = valueLoader.call();
                if (value != null) {
                    // 양쪽 캐시에 저장
                    put(key, value);
                }
                return value;
            } catch (Exception e) {
                throw new org.springframework.cache.Cache.ValueRetrievalException(key, valueLoader, e);
            }
        }

        @Override
        public void put(Object key, Object value) {
            // 양쪽 캐시에 모두 저장
            l1Cache.put(key, value);
            l2Cache.put(key, value);
            log.debug("캐시 저장: {} - {} = {}", name, key, value);
        }

        @Override
        public void evict(Object key) {
            // 양쪽 캐시에서 모두 제거
            l1Cache.evict(key);
            l2Cache.evict(key);
            log.debug("캐시 제거: {} - {}", name, key);
        }

        @Override
        public void clear() {
            // 양쪽 캐시 모두 클리어
            l1Cache.clear();
            l2Cache.clear();
            log.info("캐시 전체 클리어: {}", name);
        }

        @Override
        public ValueWrapper putIfAbsent(Object key, Object value) {
            // L1에서 먼저 확인
            ValueWrapper existing = l1Cache.get(key);
            if (existing != null) {
                return existing;
            }

            // L2에서 확인
            existing = l2Cache.get(key);
            if (existing != null) {
                // L1에도 복사
                l1Cache.put(key, existing.get());
                return existing;
            }

            // 둘 다 없으면 양쪽에 저장
            put(key, value);
            return null;
        }
    }
}