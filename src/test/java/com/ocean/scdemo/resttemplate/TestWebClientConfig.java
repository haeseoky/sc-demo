package com.ocean.scdemo.resttemplate;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@TestConfiguration
public class TestWebClientConfig {
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
            .baseUrl("http://localhost:9900") // 기본 URL 설정
            .defaultHeader("Content-Type", "application/json") // 기본 헤더 설정
            .build();
    }

    @Bean
    public WebClient optimizedWebClient() {
        // 연결 풀링 설정
        ConnectionProvider connectionProvider = ConnectionProvider.builder("webclient-conn-pool")
            .maxConnections(100) // 최대 연결 수
            .maxIdleTime(Duration.ofMinutes(1)) // 최대 유휴 시간
            .build();

        // HttpClient 설정
        HttpClient httpClient = HttpClient.create(connectionProvider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000) // 연결 타임아웃
            .doOnConnected(connection -> {
                connection.addHandlerLast(new ReadTimeoutHandler(10)); // 읽기 타임아웃
                connection.addHandlerLast(new WriteTimeoutHandler(10)); // 쓰기 타임아웃
            });

        // WebClient 생성
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .baseUrl("http://localhost:9900")
            .build();
    }
}
