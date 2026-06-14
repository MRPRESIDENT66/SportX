package com.example.sportx.Service;

public interface LeaderboardService {

    /**
     * 记录场馆热度变更并更新 Redis ZSet。
     * DB 流水写入成功（唯一约束通过）后才执行 ZINCRBY，重复事件直接幂等忽略。
     */
    void incrementSpotHeat(Long spotId, String userId, Long challengeId, String eventType, double delta);

    /**
     * 记录用户积分变更并更新 Redis ZSet。
     * DB 流水写入成功（唯一约束通过）后才执行 ZINCRBY，重复事件直接幂等忽略。
     */
    void incrementUserScore(String userId, Long challengeId, String eventType, double delta);

    /**
     * 对账：以 DB 流水 SUM 为基准修正 Redis ZSet 中的用户积分。
     * 定时任务调用，发现漂移时用 ZADD 覆写纠正。
     */
    void reconcileUserScore(String userId);

    /**
     * 对账：以 DB 流水 SUM 为基准修正 Redis ZSet 中的场馆热度。
     * 定时任务调用，发现漂移时用 ZADD 覆写纠正。
     */
    void reconcileSpotHeat(Long spotId);
}
