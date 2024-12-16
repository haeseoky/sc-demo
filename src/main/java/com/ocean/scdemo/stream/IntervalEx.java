package com.ocean.scdemo.stream;

import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@Slf4j
public class IntervalEx {

    public static void main(String[] args) {
        Publisher<Integer> pub = new Publisher<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> sub) {
                sub.onSubscribe(new Subscription() {
                    @Override
                    public void request(long n) {
                        for (long i = 0; i < 10; i++) {

                            sub.onNext((int) i);
                        }
                        sub.onComplete();
                    }

                    @Override
                    public void cancel() {
                    }
                });
            }
        };

        Publisher<Integer> subOnPub = new Publisher<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> subscriber) {
                pub.subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onSubscribe(Subscription subscription) {
                        subscriber.onSubscribe(subscription);
                    }

                    @Override
                    public void onNext(Integer integer) {
                        subscriber.onNext(integer * 10);
                    }

                    @Override
                    public void onError(Throwable throwable) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
            }
        };

        subOnPub.subscribe(new Subscriber<>() {
            @Override
            public void onSubscribe(Subscription s) {
                log.info("onSubscribe");
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(Integer i) {
                log.info("onNext: {}", i);
            }

            @Override
            public void onError(Throwable t) {
                log.info("onError: {}", t);
            }

            @Override
            public void onComplete() {
                log.info("onComplete");
            }
        });

        log.info("exit");
    }

}
