package com.ocean.scdemo.config.model;

import lombok.Getter;

@Getter
public class ExceptionResponse {
    private final String errorCode;
    private final String errorMessage;


    public ExceptionResponse(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
