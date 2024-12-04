package com.ocean.scdemo.parallel;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;


@Getter
@RedisHash("TestData")
public class TestData {
    @Id
    private String age;

    public TestData(String age) {
        this.age = age;
    }
}
