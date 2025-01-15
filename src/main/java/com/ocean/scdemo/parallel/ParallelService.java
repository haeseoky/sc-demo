package com.ocean.scdemo.parallel;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ParallelService {
//    private AtomicLong counter = new AtomicLong();

    private final TestDataRepository testDataRepository;
    private final TestRdbDataJpaRepository testRdbDataJpaRepository;
    private final Executor normalTaskExecutor;

    private final JdbcTemplate jdbcTemplate;

    public String saveByJdbcTemplate(Integer count){

        // group by chunk size(10000) from 0 to count
        // Map<Integer, List<TestRdbData>>
        Map<Integer, List<TestRdbData>> collect = IntStream.range(0, count)
            .mapToObj(i -> new TestRdbData((long)i, "name" + i))
            .collect(Collectors.groupingBy(i -> (int) (i.getId() / 10_000)));

        collect.forEach((key, value) -> {
            jdbcTemplate.batchUpdate("insert into test_rdb_data (id, name) values (?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        TestRdbData testRdbData = value.get(i);
                        ps.setLong(1, testRdbData.getId());
                        ps.setString(2, testRdbData.getName());
                    }

                    @Override
                    public int getBatchSize() {
                        return value.size();
                    }
                });
        });

        return "doSomething";
    }


    public void saveOne(TestData testData) {

        log.info("===== saveOne: {}", testData);
        TestData save = testDataRepository.save(testData);

    }

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

    public void deleteAll() {

        testRdbDataJpaRepository.deleteAllInBatch();
    }
}
