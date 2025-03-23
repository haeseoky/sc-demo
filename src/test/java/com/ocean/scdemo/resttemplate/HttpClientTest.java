package com.ocean.scdemo.resttemplate;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
public class HttpClientTest {

    @Test
    void testGet() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.exchange("http://localhost:8080/api", HttpMethod.GET, null, String.class);
    }
}
