package com.ocean.scdemo.ranking;

import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
@Disabled("Redis 서버가 필요한 테스트 - 메모리 이슈로 임시 비활성화")
class RankingServiceTest {

    @Autowired
    private RankingService rankingService;

    private static final int END = 100;

    @Test
    void addScore() {

        IntStream.range(0, END)
            .forEach(i -> {
                log.info("i: {}", i);
                rankingService.addScore("haeseoky" + i, i % 50);
            });

    }

    @Test
    void getScore() {
        IntStream.range(0, END)
            .forEach(i -> {
                Double haeseoky1 = rankingService.getScore("haeseoky" + i);
                log.info("haeseoky{}: {}",i,  haeseoky1);
            });
    }

    @Test
    void getRank() {

        IntStream.range(0, END)
            .forEach(i -> {
                Long haeseoky1 = rankingService.getRank("haeseoky" + i);
                log.info("haeseoky{}: {}",i,  haeseoky1);
            });
    }

    @Test
    void getRankCount() {
        Long rankCount = rankingService.getRankCount();
        System.out.println("rankCount = " + rankCount);
    }

    @Test
    void remove() {
        rankingService.remove("haeseoky1");
    }

    @Test
    void removeAll() {
        rankingService.removeAll();
    }
}