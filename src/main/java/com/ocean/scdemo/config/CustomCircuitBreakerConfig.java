package com.ocean.scdemo.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomCircuitBreakerConfig {

    @Bean
    public CircuitBreakerConfig circuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
            .failureRateThreshold(50) // 백분율 임계치
            .waitDurationInOpenState(Duration.ofSeconds(5)) // 오픈에서 하프오픈으로 전환되는 시간
            .permittedNumberOfCallsInHalfOpenState(2) // 하프오픈 상태에서 호출 허용 횟수
            .slidingWindowSize(5)
            .recordExceptions(Exception.class) // 기록할 예외
            .build();
    }

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(CircuitBreakerConfig circuitBreakerConfig) {
        return CircuitBreakerRegistry.of(circuitBreakerConfig);
    }
}
