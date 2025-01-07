package com.ocean.scdemo.parallel;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class NudgeService {

    private static final List<String> list = List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");

    public List<String> getNudgeList(Integer index) {
        log.info("getNudgeList: {}", index);

        List<CompletableFuture<String>> completableFutures = list.stream()
            .map(s -> {
                CompletableFuture<String> stringCompletableFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    return s + "_changed";
                });

                log.info("stringCompletableFuture: {}", stringCompletableFuture);

                return stringCompletableFuture;
            }).toList();

        List<String> result = completableFutures.stream()
            .map(CompletableFuture::join)
            .toList();

        log.info("result(count:{}): {}", result.size(), result);

        return result;
    }
}
