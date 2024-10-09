package com.ocean.scdemo.sample.domain;

import java.time.LocalDate;

public record Person(
    String name,
    String identity,
    LocalDate birth,
    String address,
    String email,
    String phone,
    // 성별
    Gender gender
) {
    public static Person createEmpty() {
        return new Person("", "",LocalDate.MAX, "", "", "", Gender.NONE);
    }
}
