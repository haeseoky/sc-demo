package com.ocean.scdemo.inject;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CService {
//    private final AService aService;
//
    public String doSomething() {
        log.info("CService.doSomething");
//        aService.doSomething();
        return "CService.doSomething";
    }

}
