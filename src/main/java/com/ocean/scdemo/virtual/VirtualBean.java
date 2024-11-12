package com.ocean.scdemo.virtual;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class VirtualBean {

    @Async("virtualTaskExecutor")
    public void getVirtualBean(int i) {
        try {

            Thread thread = Thread.currentThread();
            String name = thread.getName();
            boolean virtual = thread.isVirtual();
            log.info("VirtualBean.getVirtualBean() is called. name: {}, virtual: {}, i: {}", name, virtual, i);

            Thread.sleep(1000);
            log.info("VirtualBean.getVirtualBean() is finished");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Async("normalTaskExecutor")
    public void getNormalBean(int i) {
        try {

            Thread thread = Thread.currentThread();
            String name = thread.getName();
            boolean virtual = thread.isVirtual();
            log.info("NormalBean.getNormalBean() is called. name: {}, virtual: {}, i: {}", name, virtual, i);

            Thread.sleep(1000);
            log.info("NormalBean.getNormalBean() is finished");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
