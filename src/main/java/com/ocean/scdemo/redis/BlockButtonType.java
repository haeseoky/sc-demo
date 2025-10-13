package com.ocean.scdemo.redis;

public enum BlockButtonType {
    DEFAULT("default"),
    PRIMARY("primary");

    private final String desc;

    BlockButtonType(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
