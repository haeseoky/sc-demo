package com.ocean.scdemo.stream;

import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@Slf4j
public class CustomSubstriber implements Subscriber<Integer> {
    private Subscription subscription;
    int count = 0;
    long requestCount = 5;

    @Override
    public void onSubscribe(Subscription subscription) {
        log.info("Subscribed");
        this.subscription = subscription;
        this.subscription.request(requestCount);
    }

    @Override
    public void onNext(Integer integer) {
        log.info("Received: {}, count: {}", integer, count);


        if (++count % requestCount == 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            this.subscription.request(requestCount);
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
