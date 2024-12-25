package com.ocean.scdemo.stream;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FutureService {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newCachedThreadPool();

//        Future<String> async = executorService.submit(new Callable<String>() {
//            @Override
//            public String call() throws Exception {
//
//                Thread.sleep(2000);
//                log.info("Async");
//                return "Hello";
//            }
//        });

        FutureTask<String> futureTask = new FutureTask<>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Thread.sleep(2000);
                log.info("FutureTask");
                return "Hello";
            }
        }){
            @Override
            protected void done() {
                try {
                    log.info(get());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        };

        executorService.execute(futureTask);

//        boolean done = async.isDone();
//        log.info("exit");
//
//        String s = async.get();
//        log.info("Result: {}", s);


        executorService.shutdown();

    }

}
