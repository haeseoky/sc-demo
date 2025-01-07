package com.ocean.scdemo.parallel;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootTest
class NudgeControllerTest {

    @Autowired
    private WebClient localWebClient;


    @Test
    void test() {
        assertNotNull(localWebClient);

        localWebClient.get()
            .uri("/nudges?index=1")
            .retrieve()
            .bodyToMono(String.class)
            .subscribe(System.out::println);

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


}