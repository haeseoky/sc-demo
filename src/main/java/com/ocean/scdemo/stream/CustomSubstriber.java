package com.ocean.scdemo.stream;

import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@Slf4j
public class CustomSubstriber implements Subscriber<Integer> {
    private Subscription subscription;
    int count = 0;

    @Override
    public void onSubscribe(Subscription subscription) {
        log.info("Subscribed");
        this.subscription = subscription;
        this.subscription.request(3L);
    }

    @Override
    public void onNext(Integer integer) {
        System.out.println("Received: " + integer);

        if (++count % 3 == 0) {
            this.subscription.request(3L);
        }

    }

    @Override
    public void onError(Throwable throwable) {
        log.error("Error: ", throwable);
    }

    @Override
    public void onComplete() {
        System.out.println("Done!");
    }

}
