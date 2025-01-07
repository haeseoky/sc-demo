package com.ocean.scdemo.ranking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

    private static final String RANKING_KEY = "ranking";

    private final RedisTemplate<String, Object> redisTemplate;

    // sorted set example
    // zadd ranking 100 "Alice"
    // zadd ranking 200 "Bob"
    public void addScore(String member, double score) {
        redisTemplate.opsForZSet().add(RANKING_KEY, member, score);
    }

    public Double getScore(String member) {
        return redisTemplate.opsForZSet().score(RANKING_KEY, member);
    }

    public Long getRank(String member) {
        return redisTemplate.opsForZSet().reverseRank(RANKING_KEY, member);
    }

    public Long getRankCount() {
        return redisTemplate.opsForZSet().zCard(RANKING_KEY);
    }

    public void remove(String member) {
        redisTemplate.opsForZSet().remove(RANKING_KEY, member);
    }

    public void removeAll() {
        redisTemplate.delete(RANKING_KEY);
    }

}
