package com.ocean.scdemo.junittest.service;

import org.springframework.stereotype.Service;

@Service
public class JunitTestService {

    public int add(int a, int b) {
        return a + b;
    }
    public int subtract(int a, int b) {
        return a - b;
    }
}
