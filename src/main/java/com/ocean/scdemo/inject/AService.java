package com.ocean.scdemo.inject;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AService {
    @Autowired
    private BService bService;

    private final CService cService;

    public String doSomething() {
        log.info("AService.doSomething");

        cService.doSomething();
        return bService.doSomething();
    }

}
