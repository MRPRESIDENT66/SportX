package com.example.sportx.Service.Impl;

import com.example.sportx.Service.LeaderboardService;
import com.example.sportx.Utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class LeaderboardServiceImpl implements LeaderboardService {

    private final StringRedisTemplate stringRedisTemplate;

    public LeaderboardServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void incrementSpotHeat(Long spotId, double delta) {
        if (spotId == null) {
            return;
        }
        stringRedisTemplate.opsForZSet()
                .incrementScore(RedisConstants.LEADERBOARD_SPOT_HEAT_KEY, spotId.toString(), delta);
    }

    @Override
    public void incrementUserScore(String userId, double delta) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        stringRedisTemplate.opsForZSet()
                .incrementScore(RedisConstants.LEADERBOARD_USER_CHALLENGE_KEY, userId, delta);
    }
}
