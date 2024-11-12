package com.ocean.scdemo.virtual;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class VirtualThreadServiceTest {
    @Autowired
    private VirtualThreadService virtualThreadService;


    @Test
    void getVirtualThread() {
        System.out.println(virtualThreadService.getVirtualThread());
    }

    @Test
    void getNormalThread() {
        System.out.println(virtualThreadService.getNormalThread());
    }

}