package com.example.sportx.RabbitMQ;

import com.example.sportx.Service.LeaderboardService;
import com.example.sportx.Utils.RedisConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 排行榜对账定时任务：每小时以 DB 流水 SUM 为基准，修正 Redis ZSet 中可能漂移的 score。
 *
 * 漂移来源：极端情况下 ZINCRBY 与 DB 流水 INSERT 之间的异常（如 Redis 临时不可用），
 * 或历史遗留的非幂等消费导致重复累加。对账让 Redis 最终收敛到正确值。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LeaderboardSnapshotScheduler {

    /** 单次对账最多处理的榜单条目数，避免全量扫描压垮 Redis。 */
    private static final int RECONCILE_BATCH = 200;

    private final LeaderboardService leaderboardService;
    private final StringRedisTemplate redisTemplate;

    /** 每小时对账一次用户积分榜：读取 ZSet 前 N 名，逐个与 DB SUM 比对并修正。 */
    @Scheduled(cron = "0 0 * * * *")
    public void reconcileUserScores() {
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(RedisConstants.LEADERBOARD_USER_CHALLENGE_KEY, 0, RECONCILE_BATCH - 1);
        if (tuples == null || tuples.isEmpty()) {
            return;
        }
        log.info("Starting user score reconciliation, entries={}", tuples.size());
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            if (tuple == null || tuple.getValue() == null) continue;
            leaderboardService.reconcileUserScore(tuple.getValue());
        }
        log.info("User score reconciliation done.");
    }

    /** 每小时对账一次场馆热度榜：逻辑同上，修正热度 score。 */
    @Scheduled(cron = "0 30 * * * *")
    public void reconcileSpotHeat() {
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(RedisConstants.LEADERBOARD_SPOT_HEAT_KEY, 0, RECONCILE_BATCH - 1);
        if (tuples == null || tuples.isEmpty()) {
            return;
        }
        log.info("Starting spot heat reconciliation, entries={}", tuples.size());
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            if (tuple == null || tuple.getValue() == null) continue;
            try {
                Long spotId = Long.valueOf(tuple.getValue());
                leaderboardService.reconcileSpotHeat(spotId);
            } catch (NumberFormatException ignored) {
                log.warn("Invalid spotId in ZSet member: {}", tuple.getValue());
            }
        }
        log.info("Spot heat reconciliation done.");
    }
}
