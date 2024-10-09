package com.ocean.scdemo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Configuration
public class WebClientConfig {
    @Value("${webclient.local.base-url}")
    private String localBaseUrl;


    @Bean
    public WebClient localWebClient() {
        return WebClient.builder()
            .baseUrl(localBaseUrl)
            .build();
    }

}
