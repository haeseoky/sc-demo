package com.ocean.scdemo.stream;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Slf4j
public class FluxService {

    public static void main(String[] args) throws InterruptedException {
        Flux.range(1,3)
            .publishOn(Schedulers.newSingle("pub")) // 데이터 생성은 빠르지만, consumer가 굉장히 느릴경우 사용
            .log() //publishOn 로 부터의 로그를 받는다.
            .subscribe(System.out::println);
    }



}
