package com.ocean.scdemo.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 메소드의 중복 실행을 방지하는 어노테이션
 *
 * 파라미터 객체의 특정 속성값들을 조합하여 Redis에 키를 저장하고,
 * 해당 키가 존재하는 동안(TTL) 중복 실행을 차단합니다.
 *
 * 사용 예시:
 * <pre>
 * {@code
 * @PreventDuplicateExecution(keys = {"userId", "orderId"}, ttl = 10)
 * public void processOrder(OrderRequest request) {
 *     // orderId와 userId 조합으로 10초간 중복 실행 방지
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PreventDuplicateExecution {

    /**
     * 파라미터 객체에서 추출할 속성명 배열
     * 이 속성값들을 조합하여 Redis 키를 생성합니다.
     *
     * @return 속성명 배열
     */
    String[] keys();

    /**
     * Redis 키의 TTL(Time To Live) 시간 (초 단위)
     * 기본값: 5초
     *
     * @return TTL 시간(초)
     */
    long ttl() default 5;

    /**
     * 중복 실행 시 반환할 에러 메시지
     * 기본값: "이미 실행 중인 요청입니다. 잠시 후 다시 시도해주세요."
     *
     * @return 에러 메시지
     */
    String message() default "이미 실행 중인 요청입니다. 잠시 후 다시 시도해주세요.";

    /**
     * 파라미터가 없는 경우 메소드명만으로 키 생성 여부
     * 기본값: false
     *
     * @return 메소드명 사용 여부
     */
    boolean useMethodName() default false;
}
