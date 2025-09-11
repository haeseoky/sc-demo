package com.ocean.scdemo.sample.infrastructure;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("ElasticSearch 서버가 필요한 테스트 - 메모리 이슈로 임시 비활성화")
class ElasticRepositoryTest {
    @Autowired
    private ElasticRepository elasticRepository;


    @Test
    void createIndex() {
        try {
            elasticRepository.createIndex("shape");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}