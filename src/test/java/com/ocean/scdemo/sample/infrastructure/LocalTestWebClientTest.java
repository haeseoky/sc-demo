package com.ocean.scdemo.sample.infrastructure;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.ocean.scdemo.sample.infrastructure.model.response.SampleResponse;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
class LocalTestWebClientTest {

    @Autowired
    private LocalTestWebClient localTestWebClient;

    @Test
    void testGetSample() throws InterruptedException {

        while(true) {

            Thread.sleep(1000);
            SampleResponse sample = localTestWebClient.getSample();
            log.info("[CircuitBreaker] sample: {}", sample);
            assertNotNull(sample);
        }


    }


}