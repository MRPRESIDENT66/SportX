package com.example.sportx.Service;

import com.example.sportx.Entity.vo.SpotHeatRankingDto;
import com.example.sportx.Entity.vo.UserScoreRankingDto;

import java.util.List;

public interface LeaderboardService {

    void incrementSpotHeat(Long spotId, String userId, Long challengeId, String eventType, double delta);

    void incrementUserScore(String userId, Long challengeId, String eventType, double delta);

    void reconcileUserScore(String userId);

    void reconcileSpotHeat(Long spotId);

    List<SpotHeatRankingDto> getSpotHeatRanking(int limit);

    List<UserScoreRankingDto> getUserScoreRanking(int limit);
}
