package com.ocean.scdemo.sample.presentation.model;

import com.ocean.scdemo.sample.domain.Person;

public record ResPersonDto(
    String name,
    String address,
    String email,
    String phone
) {

    public static ResPersonDto fromDomain(Person person) {
        return new ResPersonDto(
            person.name(),
            person.address(),
            person.email(),
            person.phone()
        );
    }
}
