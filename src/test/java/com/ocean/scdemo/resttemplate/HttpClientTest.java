package com.ocean.scdemo.resttemplate;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
@Disabled("외부 HTTP 호출이 필요한 테스트 - 임시 비활성화")
public class HttpClientTest {

    @Test
    void testGet() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.exchange("http://localhost:8080/api", HttpMethod.GET, null, String.class);
    }
}
