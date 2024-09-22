package com.ocean.scdemo.junittest.service;

import org.springframework.stereotype.Service;

@Service
public class JunitTestProvider {

    public int getNumber() {
        return 1;
    }

    public void throwException() {
        throw new RuntimeException("This is a runtime exception");
    }
}
