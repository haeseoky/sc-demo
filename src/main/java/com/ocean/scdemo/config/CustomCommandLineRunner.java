package com.ocean.scdemo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class CustomCommandLineRunner implements CommandLineRunner {


    @Override
    public void run(String... args) throws Exception {
        log.trace("com.ocean.scdemo.config: TRACE!!");
        log.debug("com.ocean.scdemo.config: DEBUG!!");
        log.info("com.ocean.scdemo.config: INFO!!");
        log.warn("com.ocean.scdemo.config: WARN!!");
        log.error("com.ocean.scdemo.config: ERROR!!");
    }
}
