package com.ocean.scdemo.resttemplate;

import lombok.Getter;

@Getter
public class Base64Request {
    private String base64;

    public Base64Request(String s) {
        this.base64 = s;
    }
}
