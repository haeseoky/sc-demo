package com.ocean.scdemo.sample.domain;


import java.util.Arrays;

public enum Gender {
    MAN("MAN", "남자"), WOMAN("WOMAN", "여자"), NONE("NONE", "알수없음");

    private final String code;
    private final String desc;

    Gender(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static Gender fromCode(String dbData) {
        return Arrays.stream(Gender.values())
            .filter(gender -> gender.getCode().equals(dbData))
            .findFirst()
            .orElse(NONE);
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}

