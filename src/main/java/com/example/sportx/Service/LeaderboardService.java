package com.example.sportx.Service;

public interface LeaderboardService {

    /**
     * 提升指定场馆的热度得分。
     * @param spotId 场馆主键
     * @param delta  热度增量
     */
    void incrementSpotHeat(Long spotId, double delta);

    /**
     * 提升指定用户的积分得分。
     * @param userId 用户ID
     * @param delta  积分增量（可为负）
     */
    void incrementUserScore(String userId, double delta);
}
