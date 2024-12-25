package com.ocean.scdemo.type;

import lombok.Getter;

@Getter
public class Boy extends Children {
    private final String toy;


    public Boy() {
        super();
        toy = "car";
    }

    public Boy(String toy) {
        super();
        this.toy = toy;
    }

    public Boy(int age, String toy) {
        super(age);
        this.toy = toy;
    }
}
