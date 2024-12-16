package com.ocean.scdemo.stream;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FutureService {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newCachedThreadPool();

        Future<String> async = executorService.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {

                Thread.sleep(2000);
                log.info("Async");
                return "Hello";
            }
        });

        boolean done = async.isDone();
        log.info("exit");

        String s = async.get();
        log.info("Result: {}", s);

    }

}
