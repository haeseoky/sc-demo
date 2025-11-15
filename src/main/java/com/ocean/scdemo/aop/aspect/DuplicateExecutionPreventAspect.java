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
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 중복 실행 방지 AOP Aspect
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

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * @PreventDuplicateExecution 어노테이션이 적용된 메소드를 가로채서 중복 실행 방지 로직을 적용합니다.
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

        // 1. Redis 락 키 생성
        String lockKey = generateLockKey(joinPoint, annotation);

        log.debug("Checking duplicate execution for lockKey: {}", lockKey);

        // 2. Redis에 키가 이미 존재하는지 확인 (중복 실행 체크)
        Boolean isLocked = redisTemplate.opsForValue().setIfAbsent(
                lockKey,
                LOCK_VALUE,
                annotation.ttl(),
                TimeUnit.SECONDS
        );

        // 3. 이미 실행 중이면 예외 발생
        if (Boolean.FALSE.equals(isLocked)) {
            log.warn("Duplicate execution detected for lockKey: {}", lockKey);
            throw new DuplicateExecutionException(annotation.message(), lockKey);
        }

        try {
            // 4. 메소드 실행
            log.debug("Executing method with lockKey: {}", lockKey);
            return joinPoint.proceed();

        } finally {
            // 5. 메소드 실행 완료 후 락 해제
            redisTemplate.delete(lockKey);
            log.debug("Released lock for lockKey: {}", lockKey);
        }
    }

    /**
     * 메소드 시그니처와 파라미터를 기반으로 Redis 락 키를 생성합니다.
     *
     * @param joinPoint AOP 조인 포인트
     * @param annotation PreventDuplicateExecution 어노테이션
     * @return 생성된 락 키
     */
    private String generateLockKey(ProceedingJoinPoint joinPoint, PreventDuplicateExecution annotation) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getMethod().getName();

        // 메소드명만 사용하는 경우
        if (annotation.useMethodName()) {
            return LOCK_KEY_PREFIX + className + ":" + methodName;
        }

        // 파라미터에서 키 값 추출
        String[] keys = annotation.keys();
        Object[] args = joinPoint.getArgs();

        if (keys.length == 0) {
            throw new IllegalArgumentException(
                    "PreventDuplicateExecution annotation must have at least one key or useMethodName=true"
            );
        }

        if (args.length == 0) {
            throw new IllegalArgumentException(
                    "Method " + methodName + " has no parameters to extract keys from"
            );
        }

        // 첫 번째 파라미터에서 키 값들 추출
        Object param = args[0];
        String keyValues = extractKeyValues(param, keys);

        return LOCK_KEY_PREFIX + className + ":" + methodName + ":" + keyValues;
    }

    /**
     * 파라미터 객체에서 지정된 필드명들의 값을 추출하여 조합합니다.
     *
     * @param param 파라미터 객체
     * @param fieldNames 추출할 필드명 배열
     * @return 필드값들을 조합한 문자열
     */
    private String extractKeyValues(Object param, String[] fieldNames) {
        return Arrays.stream(fieldNames)
                .map(fieldName -> extractFieldValue(param, fieldName))
                .collect(Collectors.joining(":"));
    }

    /**
     * 파라미터 객체에서 특정 필드의 값을 추출합니다.
     * getter 메소드 또는 리플렉션을 사용하여 값을 가져옵니다.
     *
     * @param param 파라미터 객체
     * @param fieldName 필드명
     * @return 필드 값 문자열
     */
    private String extractFieldValue(Object param, String fieldName) {
        try {
            // 1. getter 메소드 시도 (getFieldName 또는 isFieldName)
            try {
                String getterName = "get" + capitalize(fieldName);
                Object value = param.getClass().getMethod(getterName).invoke(param);
                return value != null ? value.toString() : "null";
            } catch (NoSuchMethodException e) {
                // getter가 없으면 boolean 타입의 isXxx 시도
                try {
                    String isGetterName = "is" + capitalize(fieldName);
                    Object value = param.getClass().getMethod(isGetterName).invoke(param);
                    return value != null ? value.toString() : "null";
                } catch (NoSuchMethodException ex) {
                    // getter도 없으면 필드에 직접 접근
                    Field field = findField(param.getClass(), fieldName);
                    if (field != null) {
                        field.setAccessible(true);
                        Object value = field.get(param);
                        return value != null ? value.toString() : "null";
                    }
                    throw new IllegalArgumentException(
                            "Field '" + fieldName + "' not found in " + param.getClass().getSimpleName()
                    );
                }
            }
        } catch (Exception e) {
            log.error("Failed to extract field value: {}", fieldName, e);
            throw new IllegalArgumentException(
                    "Failed to extract field '" + fieldName + "' from " + param.getClass().getSimpleName(),
                    e
            );
        }
    }

    /**
     * 클래스 계층 구조를 따라 올라가면서 필드를 찾습니다.
     *
     * @param clazz 클래스
     * @param fieldName 필드명
     * @return 찾은 필드 또는 null
     */
    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
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
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
