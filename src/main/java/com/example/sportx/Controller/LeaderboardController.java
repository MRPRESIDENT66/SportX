package com.example.sportx.Controller;

import com.example.sportx.Entity.Result;
import com.example.sportx.Entity.SpotHeatRankingDto;
import com.example.sportx.Entity.Spots;
import com.example.sportx.Service.SpotsService;
import com.example.sportx.Utils.RedisConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final StringRedisTemplate stringRedisTemplate;
    private final SpotsService spotsService;

    @GetMapping("/spots/heat")
    public Result<List<SpotHeatRankingDto>> topSpotHeat(@RequestParam(value = "limit", defaultValue = "10") int limit) {
        int sanitizedLimit = Math.min(Math.max(limit, 1), 50);

        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(RedisConstants.LEADERBOARD_SPOT_HEAT_KEY, 0, sanitizedLimit - 1);
        if (tuples == null || tuples.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        List<SpotHeatRankingDto> rankings = new ArrayList<>(tuples.size());
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            if (tuple == null || tuple.getValue() == null) {
                continue;
            }
            Long spotId = parseSpotId(tuple.getValue());
            if (spotId == null) {
                continue;
            }

            Spots spot = spotsService.getById(spotId);

            SpotHeatRankingDto dto = new SpotHeatRankingDto();
            dto.setRank(rank++);
            dto.setSpotId(spotId);
            dto.setScore(tuple.getScore());
            if (spot != null) {
                dto.setSpotName(spot.getName());
                dto.setRegion(spot.getRegion());
                dto.setType(spot.getType());
            }
            rankings.add(dto);
        }

        return Result.success(rankings);
    }

    private Long parseSpotId(String raw) {
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
