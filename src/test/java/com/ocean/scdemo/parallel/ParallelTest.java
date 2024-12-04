package com.ocean.scdemo.parallel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

@SpringBootTest
@Slf4j
public class ParallelTest {

    private static List<TestData> list;
    private static final int CHUNK_SIZE = 200_000;
    private static final int endExclusive = 10_000_000;

    @Autowired
    private ParallelService parallelService;

    @BeforeAll
    static void beforeAll() {

//        list = LongStream.range(0, endExclusive)
//            .mapToObj(l -> new TestData("test"+l)).toList();

    }

    @Test
    void test1() {
        // show example chunk list


        // list make endExclusive divided by CHUNK_SIZE
        // and


        list = LongStream.range(0, endExclusive)
            .mapToObj(l -> new TestData("test"+l)).toList();

        log.info("============ availableProcessors: {}", Runtime.getRuntime().availableProcessors());

        StopWatch stopWatch = new StopWatch();
        stopWatch.start("make chunk list");
        List<List<TestData>> chunks = chunkList(list, CHUNK_SIZE);

        stopWatch.stop();
        log.info(stopWatch.prettyPrint());

        stopWatch.start("save");

        chunks.forEach(chunk -> {
            log.info("========== chunk no: {}, size: {}", chunks.indexOf(chunk), chunk.size());
            parallelService.save(chunk);
        });
        stopWatch.stop();
        log.info(stopWatch.prettyPrint());
    }

    public static <T> List<List<T>> chunkList(List<T> list, int chunkSize) {
        List<List<T>> chunks = new ArrayList<>();
        int size = list.size();
        for (int i = 0; i < size; i += chunkSize) {
            chunks.add(list.subList(i, Math.min(size, i + chunkSize)));
        }
        return chunks;
    }
}
