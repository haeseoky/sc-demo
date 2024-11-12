package com.ocean.scdemo.sample.infrastructure;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
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