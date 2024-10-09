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

    public LocalTestWebClient(WebClient localWebClient, LocalCircuitBreaker localCircuitBreaker) {
        this.localWebClient = localWebClient;
        this.localCircuitBreaker = localCircuitBreaker;
    }

    public SampleResponse getSample() {

        try {
            Supplier<SampleResponse> sampleResponseSupplier = localCircuitBreaker.get().decorateSupplier(this::getSampleResponse);
            return sampleResponseSupplier.get();
        } catch (CallNotPermittedException e) {
            log.info("[CircuitBreaker] CircuitBreaker is open");
        } catch (Exception e) {
            log.error("[CircuitBreaker] Error: {}", e.getMessage());
        }
        return SampleResponse.createEmpty();
    }

    private SampleResponse getSampleResponse() {
        return localWebClient.get()
            .uri("/api/sample")
            .retrieve()
            .bodyToMono(SampleResponse.class)
            .block();
    }
}
