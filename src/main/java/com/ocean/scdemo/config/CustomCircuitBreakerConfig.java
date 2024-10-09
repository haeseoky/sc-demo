package com.ocean.scdemo.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomCircuitBreakerConfig {

    @Bean
    public CircuitBreakerConfig circuitBreakerConfig() {
        return CircuitBreakerConfig.custom()

            // 10회 호출 중 50% 이상 실패하면 open
            // open 상태에서 2초 후 half-open
            // half-open 상태에서 2회 호출 허용
//            .slidingWindowType(SlidingWindowType.COUNT_BASED) // 호출 횟수 기반
//            .slidingWindowSize(30) // 10회간의 호출을 기록 default 100
//            .failureRateThreshold(50) // 기록된 호출중 50%가 실패하면 open default 50
//            .waitDurationInOpenState(Duration.ofSeconds(3)) // 오픈에서 하프오픈으로 전환되는 시간 default 60
//            .permittedNumberOfCallsInHalfOpenState(20) // 하프오픈 상태에서 호출 허용 횟수 default 10
//            .recordExceptions(Exception.class) // 기록할 예외


            .slidingWindowType(SlidingWindowType.TIME_BASED)
            .minimumNumberOfCalls(20) // 최소 호출 횟수 default 100
            .slidingWindowSize(30) // 30초간의 호출을 기록 default 100

            .failureRateThreshold(50) // 기록된 호출 중 50%가 실패하면 open default 50
            .waitDurationInOpenState(Duration.ofSeconds(3)) // 오픈에서 하프오픈으로 전환되는 시간 default 60
            .permittedNumberOfCallsInHalfOpenState(20) // 하프오픈 상태에서 호출 허용 횟수 default 10
            .recordExceptions(Exception.class)


//            .slowCallDurationThreshold(Duration.ofSeconds(3)) // 느린 호출 임계치
//            .slowCallRateThreshold(50) // 기록된 호출 중 50%가 느린 호출이면 open

            .build();
    }

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(CircuitBreakerConfig circuitBreakerConfig) {
        return CircuitBreakerRegistry.of(circuitBreakerConfig);
    }
}
