package com.ocean.scdemo.sample.infrastructure;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.ocean.scdemo.sample.infrastructure.model.response.SampleResponse;
import java.util.UUID;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
class LocalTestWebClientTest {

    @Autowired
    private LocalTestWebClient localTestWebClient;

    @Test
    void testGetSample() throws InterruptedException {

        int count = 1;

        while(true) {
            // lpad with 0
            String countStr = String.format("%03d", count++);
            MDC.put("traceId", countStr+": "+UUID.randomUUID().toString());

            Thread.sleep(500);
            SampleResponse sample = localTestWebClient.getSample("/api/sample");
            log.info("[CircuitBreaker] sample: {}", sample);
            assertNotNull(sample);
        }


    }


}