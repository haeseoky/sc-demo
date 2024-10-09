package com.ocean.scdemo.sample.infrastructure;

import com.ocean.scdemo.config.circuitbreaker.LocalCircuitBreaker;
import com.ocean.scdemo.sample.infrastructure.model.response.SampleResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class LocalTestWebClient {

    private final WebClient localWebClient;
    private final LocalCircuitBreaker localCircuitBreaker;

    public LocalTestWebClient(
        WebClient localWebClient,
        LocalCircuitBreaker localCircuitBreaker
    ) {
        this.localWebClient = localWebClient;
        this.localCircuitBreaker = localCircuitBreaker;
    }

    public SampleResponse getSample(String str) {
        return execute(localCircuitBreaker, () -> getSampleResponse(str), this::fallback);
    }

    private SampleResponse getSampleResponse(String param) {
        return localWebClient.get()
            .uri(param)
            .retrieve()
            .bodyToMono(SampleResponse.class)
            .block();
    }

    public <T> T execute(LocalCircuitBreaker circuitBreaker, Supplier<T> supplier, Supplier<T> fallback) {
        try {
            T t = circuitBreaker.get().decorateSupplier(supplier).get();
            circuitBreaker.logPrintCount();
            return t;
        } catch (CallNotPermittedException e) {
            log.info("[CircuitBreaker] CallNotPermittedException: ", e);
            circuitBreaker.logPrintCount();
            return fallback.get();
        } catch (Exception e) {
            log.error("[CircuitBreaker] Exception: ", e);
            circuitBreaker.logPrintCount();
            return fallback.get();
        }
    }

    private SampleResponse fallback() {
        log.info("[CircuitBreaker] fallback");
        return SampleResponse.createEmpty();
    }
}
