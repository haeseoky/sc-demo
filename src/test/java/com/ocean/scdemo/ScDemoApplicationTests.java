package com.ocean.scdemo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = ScDemoApplication.class)
@Profile("local")
class ScDemoApplicationTests {

    @Test
    void contextLoads() {
    }

}
