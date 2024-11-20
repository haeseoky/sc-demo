package com.ocean.scdemo.finaltest.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ToString
@Getter
@RequiredArgsConstructor
public class FinalRequest {
    private final String name;
    private final int age;

    public FinalRequest() {
        this.name = "default";
        this.age = 999;
    }

    public FinalClass toFinalClass() {

        return new FinalClass(this.name, this.age);
    }
}
