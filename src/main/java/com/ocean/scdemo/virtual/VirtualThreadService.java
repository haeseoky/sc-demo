package com.ocean.scdemo.virtual;

import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

@Slf4j
@Service
@RequiredArgsConstructor
public class VirtualThreadService {
    private final VirtualBean virtualBean;

    private static final int LOOP_COUNT = 10;

    public String getVirtualThread() {

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        IntStream.range(0, LOOP_COUNT).forEach(i -> {

            virtualBean.getVirtualBean(i);
        });

        stopWatch.stop();
        log.info("getVirtualBean Elapsed Time: {}", stopWatch.getTotalTimeSeconds());

        return "getVirtualThread";
    }

    public String getNormalThread() {

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        IntStream.range(0, LOOP_COUNT).forEach(i -> {

            log.info("getNormalThread() is called. i: {}", i);
            virtualBean.getNormalBean(i);
            log.info("getNormalThread() is finished. i: {}", i);
        });

        stopWatch.stop();
        log.info("getNormalBean Elapsed Time: {}", stopWatch.getTotalTimeSeconds());
        return "getNormalThread";
    }
}
