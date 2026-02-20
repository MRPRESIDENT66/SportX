package com.example.sportx.Controller;

import com.example.sportx.Entity.vo.Result;
import com.example.sportx.Entity.vo.SpotHeatRankingDto;
import com.example.sportx.Entity.vo.UserScoreRankingDto;
import com.example.sportx.Entity.Spots;
import com.example.sportx.Entity.User;
import com.example.sportx.Service.SpotsService;
import com.example.sportx.Service.UserService;
import com.example.sportx.Utils.RedisConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.validation.annotation.Validated;
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
@Validated
@Tag(name = "Leaderboard", description = "Ranking APIs based on Redis ZSet")
public class LeaderboardController {

    private final StringRedisTemplate stringRedisTemplate;
    private final SpotsService spotsService;
    private final UserService userService;

    @GetMapping("/spots/heat")
    @Operation(summary = "Spot heat ranking", description = "Get top spots ranked by heat score")
    public Result<List<SpotHeatRankingDto>> topSpotHeat(
            @RequestParam(value = "limit", defaultValue = "10") @Min(value = 1, message = "limit最小为1") @Max(value = 50, message = "limit最大为50") int limit) {
        // limit 二次保护，避免极端参数影响查询性能。
        int sanitizedLimit = Math.min(Math.max(limit, 1), 50);

        // 从 ZSet 读取热度倒序榜单。
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

            // 榜单存的是 spotId，需要回库补全场馆名称/区域等展示信息。
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
        // Redis member -> Long spotId 的安全转换，格式异常时跳过该条。
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @GetMapping("/users/score")
    @Operation(summary = "User score ranking", description = "Get top users ranked by challenge score")
    public Result<List<UserScoreRankingDto>> topUserScore(
            @RequestParam(value = "limit", defaultValue = "10") @Min(value = 1, message = "limit最小为1") @Max(value = 50, message = "limit最大为50") int limit) {
        // 用户积分榜同样基于 ZSet 倒序读取。
        int sanitizedLimit = Math.min(Math.max(limit, 1), 50);

        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(RedisConstants.LEADERBOARD_USER_CHALLENGE_KEY, 0, sanitizedLimit - 1);
        if (tuples == null || tuples.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        List<UserScoreRankingDto> rankings = new ArrayList<>(tuples.size());
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            if (tuple == null || tuple.getValue() == null) {
                continue;
            }
            String userId = tuple.getValue();
            // 榜单存 userId，回库补充用户昵称/城市等展示字段。
            User user = userService.getById(userId);

            UserScoreRankingDto dto = new UserScoreRankingDto();
            dto.setRank(rank++);
            dto.setUserId(userId);
            dto.setScore(tuple.getScore());
            if (user != null) {
                dto.setNickname(user.getNickname());
                dto.setCity(user.getCity());
            }
            rankings.add(dto);
        }
        // 返回的是“排行榜展示 DTO”，不是用户实体，避免过多字段泄露。
        return Result.success(rankings);
    }
}
