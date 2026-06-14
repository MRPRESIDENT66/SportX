package com.example.sportx.Service.Impl;

import com.example.sportx.Entity.LeaderboardEventLog;
import com.example.sportx.Mapper.LeaderboardEventLogMapper;
import com.example.sportx.Service.LeaderboardService;
import com.example.sportx.Utils.RedisConstants;
import com.example.sportx.Utils.RedisIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardServiceImpl implements LeaderboardService {

    private final StringRedisTemplate stringRedisTemplate;
    private final LeaderboardEventLogMapper eventLogMapper;
    private final RedisIdGenerator redisIdGenerator;

    @Override
    public void incrementSpotHeat(Long spotId, String userId, Long challengeId, String eventType, double delta) {
        if (spotId == null) {
            return;
        }
        // 先写 DB 流水：唯一约束 (userId, challengeId, eventType) 兜住重复事件。
        // INSERT 成功 → 此事件从未处理过 → 安全执行 ZINCRBY。
        // DuplicateKeyException → 已处理过 → 幂等跳过，ZSet 不动。
        if (!insertLog(userId, challengeId, spotId, eventType, 0D, delta)) {
            return;
        }
        stringRedisTemplate.opsForZSet()
                .incrementScore(RedisConstants.LEADERBOARD_SPOT_HEAT_KEY, spotId.toString(), delta);
    }

    @Override
    public void incrementUserScore(String userId, Long challengeId, String eventType, double delta) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        if (!insertLog(userId, challengeId, null, eventType, delta, 0D)) {
            return;
        }
        stringRedisTemplate.opsForZSet()
                .incrementScore(RedisConstants.LEADERBOARD_USER_CHALLENGE_KEY, userId, delta);
    }

    @Override
    public void reconcileUserScore(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        // DB 流水 SUM 是事实来源；Redis ZSet score 是衍生数据。
        // 两者不一致时用 ZADD 覆写 Redis，消除漂移。
        double expected = eventLogMapper.sumUserScore(userId);
        Double actual = stringRedisTemplate.opsForZSet()
                .score(RedisConstants.LEADERBOARD_USER_CHALLENGE_KEY, userId);
        if (actual == null || Math.abs(actual - expected) > 0.001) {
            stringRedisTemplate.opsForZSet()
                    .add(RedisConstants.LEADERBOARD_USER_CHALLENGE_KEY, userId, expected);
            log.warn("Reconciled user score: userId={} redis={} db={}", userId, actual, expected);
        }
    }

    @Override
    public void reconcileSpotHeat(Long spotId) {
        if (spotId == null) {
            return;
        }
        double expected = eventLogMapper.sumSpotHeat(spotId);
        Double actual = stringRedisTemplate.opsForZSet()
                .score(RedisConstants.LEADERBOARD_SPOT_HEAT_KEY, spotId.toString());
        if (actual == null || Math.abs(actual - expected) > 0.001) {
            stringRedisTemplate.opsForZSet()
                    .add(RedisConstants.LEADERBOARD_SPOT_HEAT_KEY, spotId.toString(), expected);
            log.warn("Reconciled spot heat: spotId={} redis={} db={}", spotId, actual, expected);
        }
    }

    /**
     * 写 DB 流水，返回 true 表示首次写入（后续应执行 ZINCRBY），
     * 返回 false 表示唯一约束冲突（重复事件，幂等跳过）。
     */
    private boolean insertLog(String userId, Long challengeId, Long spotId,
                              String eventType, double userDelta, double spotDelta) {
        LeaderboardEventLog log = new LeaderboardEventLog();
        log.setId(redisIdGenerator.nextId("lb-event"));
        log.setUserId(userId);
        log.setChallengeId(challengeId);
        log.setSpotId(spotId);
        log.setEventType(eventType);
        log.setUserDelta(userDelta);
        log.setSpotDelta(spotDelta);
        try {
            eventLogMapper.insert(log);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }
}
