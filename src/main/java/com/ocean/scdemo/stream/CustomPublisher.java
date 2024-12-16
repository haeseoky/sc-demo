package com.ocean.scdemo.stream;

import java.util.Iterator;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@Slf4j
public class CustomPublisher implements Publisher<Integer> {

    Iterator<Integer> iterator = IntStream.range(0, 20).iterator();

    @Override
    public void subscribe(Subscriber<? super Integer> subscriber) {
        subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long l) {
                log.info("Request for : " + l);
                long n = 0;
                if (!iterator.hasNext()) {
                    subscriber.onComplete();
                }

                while (n++ <= l && iterator.hasNext()) {
                    log.info("n: {}" , n);
                    subscriber.onNext(iterator.next());
                }
            }

            @Override
            public void cancel() {

            }
        });
    }
}
