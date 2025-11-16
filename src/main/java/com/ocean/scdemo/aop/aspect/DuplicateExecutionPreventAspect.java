package com.ocean.scdemo.aop.aspect;

import com.ocean.scdemo.aop.annotation.PreventDuplicateExecution;
import com.ocean.scdemo.aop.exception.DuplicateExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 중복 실행 방지 AOP Aspect
 * <p>
 * 성능 최적화:
 * - 필드 접근자 캐싱 (ConcurrentHashMap)
 * - String 타입만 지원 (리플렉션 부하 최소화)
 * - StringBuilder 기반 문자열 연결
 * - Early return 패턴 적용
 *
 * @PreventDuplicateExecution 어노테이션이 적용된 메소드의 중복 실행을 Redis를 통해 방지합니다.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DuplicateExecutionPreventAspect {

    private static final String LOCK_KEY_PREFIX = "execution:lock:";
    private static final String LOCK_VALUE = "LOCKED";
    private static final String KEY_SEPARATOR = ":";
    private static final String NULL_VALUE = "null";

    // 필드 접근자 캐시 (클래스명:필드명 -> 접근자)
    private static final ConcurrentHashMap<String, FieldAccessor> FIELD_ACCESSOR_CACHE = new ConcurrentHashMap<>(128);

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 중복 실행 방지 로직을 적용합니다.
     *
     * @param joinPoint AOP 조인 포인트
     * @param annotation PreventDuplicateExecution 어노테이션
     * @return 메소드 실행 결과
     * @throws Throwable 메소드 실행 중 발생한 예외
     */
    @Around("@annotation(annotation)")
    public Object preventDuplicateExecution(
            ProceedingJoinPoint joinPoint,
            PreventDuplicateExecution annotation
    ) throws Throwable {

        final String lockKey = generateLockKey(joinPoint, annotation);

        if (log.isDebugEnabled()) {
            log.debug("Checking duplicate execution for lockKey: {}", lockKey);
        }

        if (!acquireLock(lockKey, annotation.ttl())) {
            log.warn("Duplicate execution detected for lockKey: {}", lockKey);
            throw new DuplicateExecutionException(annotation.message(), lockKey);
        }

        try {
            if (log.isDebugEnabled()) {
                log.debug("Executing method with lockKey: {}", lockKey);
            }
            return joinPoint.proceed();
        } finally {
            releaseLock(lockKey);
        }
    }

    /**
     * Redis 락을 획득합니다.
     *
     * @param lockKey 락 키
     * @param ttl TTL (초)
     * @return 락 획득 성공 여부
     */
    private boolean acquireLock(String lockKey, long ttl) {
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                lockKey,
                LOCK_VALUE,
                ttl,
                TimeUnit.SECONDS
        );
        return Boolean.TRUE.equals(acquired);
    }

    /**
     * Redis 락을 해제합니다.
     *
     * @param lockKey 락 키
     */
    private void releaseLock(String lockKey) {
        redisTemplate.delete(lockKey);
        if (log.isDebugEnabled()) {
            log.debug("Released lock for lockKey: {}", lockKey);
        }
    }

    /**
     * Redis 락 키를 생성합니다.
     * <p>
     * 성능 최적화: StringBuilder 사용, Early return
     *
     * @param joinPoint AOP 조인 포인트
     * @param annotation PreventDuplicateExecution 어노테이션
     * @return 생성된 락 키
     */
    private String generateLockKey(ProceedingJoinPoint joinPoint, PreventDuplicateExecution annotation) {
        final MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        final String className = signature.getDeclaringType().getSimpleName();
        final String methodName = signature.getMethod().getName();

        final StringBuilder keyBuilder = new StringBuilder(128)
                .append(LOCK_KEY_PREFIX)
                .append(className)
                .append(KEY_SEPARATOR)
                .append(methodName);

        if (annotation.useMethodName()) {
            return keyBuilder.toString();
        }

        final String[] keys = annotation.keys();
        final Object[] args = joinPoint.getArgs();
        final Object param = args[0];
        appendKeyValues(keyBuilder, param, keys);

        return keyBuilder.toString();
    }

    /**
     * 파라미터 객체에서 필드값들을 추출하여 키 빌더에 추가합니다.
     * <p>
     * 성능 최적화: Stream 제거, StringBuilder 직접 사용
     *
     * @param keyBuilder 키 빌더
     * @param param 파라미터 객체
     * @param fieldNames 필드명 배열
     */
    private void appendKeyValues(StringBuilder keyBuilder, Object param, String[] fieldNames) {
        for (String fieldName : fieldNames) {
            keyBuilder.append(KEY_SEPARATOR);
            String fieldValue = extractFieldValue(param, fieldName);
            keyBuilder.append(fieldValue);
        }
    }

    /**
     * 파라미터 객체에서 String 타입 필드의 값을 추출합니다.
     * <p>
     * 성능 최적화:
     * - 필드 접근자 캐싱
     * - String 타입만 지원 (타입 검증)
     * - 계층 구조 탐색 제거 (단일 계층만)
     * - 중첩 try-catch 제거
     *
     * @param param 파라미터 객체
     * @param fieldName 필드명
     * @return 필드 값 (null인 경우 "null" 문자열)
     */
    private String extractFieldValue(Object param, String fieldName) {
        final Class<?> paramClass = param.getClass();
        final String cacheKey = buildCacheKey(paramClass, fieldName);

        // 캐시에서 접근자 조회 (없으면 생성)
        final FieldAccessor accessor = FIELD_ACCESSOR_CACHE.computeIfAbsent(
                cacheKey,
                key -> createFieldAccessor(paramClass, fieldName)
        );

        return accessor.getValue(param);
    }

    /**
     * 캐시 키를 생성합니다.
     *
     * @param paramClass 파라미터 클래스
     * @param fieldName 필드명
     * @return 캐시 키
     */
    private String buildCacheKey(Class<?> paramClass, String fieldName) {
        return paramClass.getName() + KEY_SEPARATOR + fieldName;
    }

    /**
     * 필드 접근자를 생성합니다.
     * <p>
     * 우선순위: getter 메소드 > 직접 필드 접근
     *
     * @param paramClass 파라미터 클래스
     * @param fieldName 필드명
     * @return 필드 접근자
     */
    private FieldAccessor createFieldAccessor(Class<?> paramClass, String fieldName) {
        // 1. getter 메소드 시도
        Method getter = findGetter(paramClass, fieldName);
        if (getter != null) {
            validateStringType(getter.getReturnType(), fieldName, paramClass);
            return new MethodAccessor(getter);
        }

        // 2. 직접 필드 접근 시도 (단일 계층만)
        Field field = findField(paramClass, fieldName);
        if (field != null) {
            validateStringType(field.getType(), fieldName, paramClass);
            field.setAccessible(true);
            return new DirectFieldAccessor(field);
        }

        throw new IllegalArgumentException(
                "Field '" + fieldName + "' not found in " + paramClass.getSimpleName()
        );
    }

    /**
     * getter 메소드를 찾습니다.
     *
     * @param paramClass 파라미터 클래스
     * @param fieldName 필드명
     * @return getter 메소드 또는 null
     */
    private Method findGetter(Class<?> paramClass, String fieldName) {
        final String getterName = "get" + capitalize(fieldName);
        try {
            return paramClass.getMethod(getterName);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * 필드를 찾습니다 (단일 계층만).
     *
     * @param paramClass 파라미터 클래스
     * @param fieldName 필드명
     * @return 필드 또는 null
     */
    private Field findField(Class<?> paramClass, String fieldName) {
        try {
            return paramClass.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    /**
     * String 타입인지 검증합니다.
     *
     * @param type 타입
     * @param fieldName 필드명
     * @param paramClass 파라미터 클래스
     */
    private void validateStringType(Class<?> type, String fieldName, Class<?> paramClass) {
        if (!String.class.equals(type)) {
            throw new IllegalArgumentException(
                    "Field '" + fieldName + "' in " + paramClass.getSimpleName() +
                    " must be of type String, but was " + type.getSimpleName()
            );
        }
    }

    /**
     * 문자열의 첫 글자를 대문자로 변환합니다.
     *
     * @param str 원본 문자열
     * @return 첫 글자가 대문자인 문자열
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        char firstChar = str.charAt(0);
        if (Character.isUpperCase(firstChar)) {
            return str;
        }
        return Character.toUpperCase(firstChar) + str.substring(1);
    }

    /**
     * 필드 접근자 인터페이스
     */
    private interface FieldAccessor {
        String getValue(Object target);
    }

    /**
     * getter 메소드 기반 접근자
     */
    private static class MethodAccessor implements FieldAccessor {
        private final Method method;

        MethodAccessor(Method method) {
            this.method = method;
        }

        @Override
        public String getValue(Object target) {
            try {
                Object value = method.invoke(target);
                return value != null ? (String) value : NULL_VALUE;
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Failed to invoke getter: " + method.getName(),
                        e
                );
            }
        }
    }

    /**
     * 직접 필드 접근자
     */
    private static class DirectFieldAccessor implements FieldAccessor {
        private final Field field;

        DirectFieldAccessor(Field field) {
            this.field = field;
        }

        @Override
        public String getValue(Object target) {
            try {
                Object value = field.get(target);
                return value != null ? (String) value : NULL_VALUE;
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(
                        "Failed to access field: " + field.getName(),
                        e
                );
            }
        }
    }
}
