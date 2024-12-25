package com.ocean.scdemo.type;

import lombok.Getter;

@Getter
public class Girl extends Children {
    private final String doll;

    public Girl() {
        this.doll = "barbie";
    }

    public Girl(String doll) {
        super();
        this.doll = doll;
    }

    public Girl(int age, String doll) {
        super(age);
        this.doll = doll;
    }
}
