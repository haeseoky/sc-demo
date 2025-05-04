package com.ocean.scdemo.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ElasticConfig {

    @Value("${elasticsearch.host:localhost}")
    private String host;

    @Value("${elasticsearch.port:9200}")
    private int port;

    @Value("${elasticsearch.protocol:http}")
    private String protocol;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        String connectionUrl = String.format("%s://%s:%d", protocol, host, port);
        
        RestClient restClient = RestClient.builder(
            HttpHost.create(connectionUrl)
        ).build();

        RestClientTransport restClientTransport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(restClientTransport);
    }
}
