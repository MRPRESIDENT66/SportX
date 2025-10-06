package com.example.sportx.Service;

public interface LeaderboardService {

    /**
     * 提升指定场馆的热度得分。
     * @param spotId 场馆主键
     * @param delta  热度增量
     */
    void incrementSpotHeat(Long spotId, double delta);
}

