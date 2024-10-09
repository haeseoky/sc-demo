package com.ocean.scdemo.config.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LocalCircuitBreaker {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    private final CircuitBreaker circuitBreaker;

    private static final String CIRCUIT_BREAKER_NAME = "localCircuitBreaker";

    public LocalCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.circuitBreaker = setCircuitBreaker();
    }

    private CircuitBreaker setCircuitBreaker() {
        addRegistryEvent();
        CircuitBreaker circuitBreaker1 = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME);
        circuitBreaker1
            .getEventPublisher()
            .onSuccess(event -> log.info("{}: CircuitBreaker onSuccess: {}", CIRCUIT_BREAKER_NAME, event))
            .onError(event -> log.info("{}: CircuitBreaker onError: {}", CIRCUIT_BREAKER_NAME, event))
            .onStateTransition(event -> log.info("{}: CircuitBreaker onStateTransition: {}", CIRCUIT_BREAKER_NAME, event))
            .onReset(event -> log.info("{}: CircuitBreaker onReset: {}", CIRCUIT_BREAKER_NAME, event))
            .onIgnoredError(event -> log.info("{}: CircuitBreaker onIgnoredError: {}", CIRCUIT_BREAKER_NAME, event))
            .onCallNotPermitted(event -> log.info("{}: CircuitBreaker onCallNotPermitted: {}", CIRCUIT_BREAKER_NAME, event))
            .onFailureRateExceeded(event -> log.info("{}: CircuitBreaker onFailureRateExceeded: {}", CIRCUIT_BREAKER_NAME, event))
            .onSlowCallRateExceeded(event -> log.info("{}: CircuitBreaker onSlowCallRateExceeded: {}", CIRCUIT_BREAKER_NAME, event));
        return circuitBreaker1;
    }

    private void addRegistryEvent() {
        circuitBreakerRegistry.getEventPublisher()
            .onEntryAdded(event -> {
                CircuitBreaker addedEntry = event.getAddedEntry();
                log.info("{}: CircuitBreaker onEntryAdded:  {}", addedEntry.getName(), event);
            })
            .onEntryRemoved(event -> {
                CircuitBreaker removedEntry = event.getRemovedEntry();
                log.info("{}: CircuitBreaker onEntryRemoved:  {}", removedEntry.getName(), event);
            })
            .onEntryReplaced(event -> {
                CircuitBreaker newEntry = event.getNewEntry();
                CircuitBreaker oldEntry = event.getOldEntry();
                log.info("{}: Old CircuitBreaker,{}: new CircuitBreaker", oldEntry.getName(), newEntry.getName());
            });
    }

    public CircuitBreaker get() {
        return circuitBreaker;
    }

}
