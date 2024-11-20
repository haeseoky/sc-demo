package com.ocean.scdemo.finaltest.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
@RequiredArgsConstructor
public class FinalClass {

    private final String name;
    private final int age;

}
