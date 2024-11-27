package com.ocean.scdemo.inject;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BService {
    private final CService cService;

    public String doSomething() {
        log.info("BService.doSomething");
        cService.doSomething();
        return "BService.doSomething";
    }
}
