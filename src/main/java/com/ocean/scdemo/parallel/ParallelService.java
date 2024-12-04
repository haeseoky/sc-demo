package com.ocean.scdemo.parallel;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParallelService {
//    private AtomicLong counter = new AtomicLong();

    private final TestDataRepository testDataRepository;
    private final Executor normalTaskExecutor;

    public String save(List<TestData> testDataList) {

        log.info("===== save: {}", testDataList.size());

//        ForkJoinPool forkJoinPool = new ForkJoinPool(4);
//        forkJoinPool.submit(() -> {
//            testDataList.forEach(testData -> {
//                CompletableFuture.supplyAsync(() -> testDataRepository.save(testData), normalTaskExecutor)
//                    .thenAccept(save -> {
//                        log.info("===== save: {}", save);
//                    });
//            });
//        });
//        forkJoinPool.submit(() -> {
//            testDataList.parallelStream().forEach(testData -> {
//                TestData save = testDataRepository.save(testData);
//            });
//        });

        // use reactive programming
        // testDataList save in parallel
//        testDataList.forEach(testData -> {
//            CompletableFuture.supplyAsync(() -> testDataRepository.save(testData), normalTaskExecutor)
//                .thenAccept(save -> {
//                    log.info("===== save: {}", save);
//                });
//        });


//        List<CompletableFuture<TestData>> collect = testDataList.stream()
//            .map(i -> CompletableFuture.supplyAsync(() -> testDataRepository.save(i), normalTaskExecutor)).toList();
//
//        List<TestData> list = collect.stream().map(CompletableFuture::join).toList();

        testDataList.parallelStream().forEach(testData -> {

            TestData save = testDataRepository.save(testData);
//            log.info("===== save: {}", save.getAge());

//            if (testData.getAge() % 10_000 == 0) {
//                log.info("===== age: {}", testData.getAge());
//            }
//            counter.incrementAndGet();

        });
        return "doSomething";
    }
}
