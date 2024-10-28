package com.ocean.scdemo;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@Slf4j
@SpringBootApplication
public class ScDemoApplication {

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(ScDemoApplication.class, args);

        // show all beans
        String[] beans = context.getBeanDefinitionNames();
        Arrays.sort(beans);
        int index = 1;
        for (String bean : beans) {
            log.info("{}. {}", String.format("%010d", index++), bean);
        }
    }
}
