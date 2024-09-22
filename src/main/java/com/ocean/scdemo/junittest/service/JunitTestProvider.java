package com.ocean.scdemo.junittest.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JunitTestProvider {


    public int getNumber() {
        log.info("Returning number 1");
        return 1;
    }

    public void throwException() {
        throw new RuntimeException("This is a runtime exception");
    }
}
