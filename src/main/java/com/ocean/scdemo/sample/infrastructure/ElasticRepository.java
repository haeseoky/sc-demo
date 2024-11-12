package com.ocean.scdemo.sample.infrastructure;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ElasticRepository {

    private final ElasticsearchClient elasticsearchClient;

    public void createIndex(String indexName) throws IOException {
        log.info("Creating index: {}", indexName);
        elasticsearchClient.indices()
            .create(createIndexRequest -> createIndexRequest.index(indexName));
    }

    public void indexDocument(String indexName, String document) throws IOException {
        log.info("Indexing document: {}", document);
        IndexResponse index = elasticsearchClient.index(indexRequest -> indexRequest.index(indexName));
        log.info("Indexed document: {}", index);
    }
}
