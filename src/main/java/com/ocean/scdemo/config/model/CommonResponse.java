package com.ocean.scdemo.config.model;


import lombok.Getter;

@Getter
public class CommonResponse<T> {
    private final T payload;

    public CommonResponse(T body) {
        this.payload = body;
    }
    
    public static <T> CommonResponse<T> success(T payload) {
        return new CommonResponse<>(payload);
    }
}
