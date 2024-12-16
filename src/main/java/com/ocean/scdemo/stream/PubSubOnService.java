package com.ocean.scdemo.stream;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

public class PubSubOnService {


    public static void main(String[] args) {

        // publishon: fast publisher, slow subscriber -> subscriber on different thread
        // ex. db, network call
        // subscribeon: slow publisher, fast subscriber -> publisher on different thread
        // ex. file read, cpu intensive task

        Flux.range(1, 10)
//            .publishOn(Schedulers.newSingle("pub"))
            .log()
            .subscribeOn(Schedulers.newSingle("sub"))
            .subscribe(s -> System.out.println("onNext: " + s));
    }

}
