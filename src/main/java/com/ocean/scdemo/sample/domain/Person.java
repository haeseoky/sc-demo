package com.ocean.scdemo.sample.domain;

public record Person(
    String name,
    int age,
    String address,
    String email,
    String phone,
    // 성별
    Gender gender
) {

}
