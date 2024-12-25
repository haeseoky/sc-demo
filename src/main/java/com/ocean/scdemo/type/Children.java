package com.ocean.scdemo.type;

import lombok.Getter;

@Getter
abstract public class Children {
    private final int age;

    public Children() {
        this.age = 99;
    }

    public Children(int age) {
        this.age = age;
    }
}
