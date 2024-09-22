package com.ocean.scdemo.sample.infrastructure;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class LogTester {

    @PostConstruct
    public void init() {
        log();
    }

    public void log() {
        log.trace("com.ocean.scdemo.sample.infrastructure: TRACE!!");
        log.debug("com.ocean.scdemo.sample.infrastructure: DEBUG!!");
        log.info("com.ocean.scdemo.sample.infrastructure: INFO!!");
        log.warn("com.ocean.scdemo.sample.infrastructure: WARN!!");
        log.error("com.ocean.scdemo.sample.infrastructure: ERROR!!");
    }

}
