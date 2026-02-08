package com.example.sportx.Service.Impl;

import com.example.sportx.Utils.RedisConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceImplTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @InjectMocks
    private LeaderboardServiceImpl leaderboardService;

    @Test
    void incrementSpotHeat_shouldCallRedisZSetIncrementScore() {
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);

        leaderboardService.incrementSpotHeat(1001L, 1.0);

        verify(zSetOperations).incrementScore(
                RedisConstants.LEADERBOARD_SPOT_HEAT_KEY,
                "1001",
                1.0
        );
    }

    @Test
    void incrementSpotHeat_shouldDoNothingWhenSpotIdIsNull() {
        leaderboardService.incrementSpotHeat(null, 1.0);

        verify(stringRedisTemplate, never()).opsForZSet();
    }

    @Test
    void incrementSpotHeat_shouldSupportNegativeDelta() {
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);

        leaderboardService.incrementSpotHeat(1001L, -0.5);

        verify(zSetOperations).incrementScore(
                RedisConstants.LEADERBOARD_SPOT_HEAT_KEY,
                "1001",
                -0.5
        );
    }
}
