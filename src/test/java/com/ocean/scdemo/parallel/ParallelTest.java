package com.ocean.scdemo.parallel;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.LongStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StopWatch;

@SpringBootTest
@Slf4j
public class ParallelTest {

    private static List<TestData> list;
    private static final int CHUNK_SIZE = 200_000;
    private static final int endExclusive = 10_000_000;

    @Autowired
    private ParallelService parallelService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestRdbDataJpaRepository testRdbDataJpaRepository;

    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;

    @BeforeEach
    void beforeEach() {

//
////        List<TestRdbData> list = LongStream.range(0, 10_000)
////            .mapToObj(l -> new TestRdbData(l, "test" + l)).toList();
////
////        testRdbDataJpaRepository.saveAllAndFlush(list);
//
//        jdbcTemplate.batchUpdate("insert into test_rdb_data (id, name) values (?, ?)",
//            new BatchPreparedStatementSetter() {
//                @Override
//                public void setValues(PreparedStatement ps, int i) throws SQLException {
//                    ps.setLong(1, i);
//                    ps.setString(2, "test" + i);
//                }
//
//                @Override
//                public int getBatchSize() {
//                    return 100_000;
//                }
//            });

    }


    @AfterEach
    void afterEach() {
//        personJpaRepository.deleteAll();
    }

    @BeforeAll
    static void beforeAll() {

//        list = LongStream.range(0, endExclusive)
//            .mapToObj(l -> new TestData("test"+l)).toList();

    }

    @Test
    void test2() {

        StopWatch stopWatch = new StopWatch();
        stopWatch.start("save");

            sqlSessionTemplate.select(
                "com.ocean.scdemo.parallel.TestRdbDataMapper.findAll",
//            null,
//            new RowBounds(0, 10_000),
                resultContext -> {
                    if (resultContext.getResultCount() % 10_000 == 0) {
                        log.info("resultCount: {}", resultContext.getResultCount());
                    }

                    TestRdbData resultObject = (TestRdbData) resultContext.getResultObject();
                    parallelService.saveOne(new TestData(resultObject.getName()));


                }
            );




        stopWatch.stop();
        log.info(stopWatch.prettyPrint());


//        List<Object> objects = sqlSessionTemplate.selectList("com.ocean.scdemo.parallel.TestRdbDataMapper.findAll");
//        log.info("objects.size: {}", objects.size());
    }


    @Test
    void test1() {
        // show example chunk list

        // list make endExclusive divided by CHUNK_SIZE
        // and

        list = LongStream.range(0, endExclusive)
            .mapToObj(l -> new TestData("test" + l)).toList();

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
