package com.example.sportx.Controller;

import com.example.sportx.Entity.vo.Result;
import com.example.sportx.Entity.vo.SpotHeatRankingDto;
import com.example.sportx.Entity.vo.UserScoreRankingDto;
import com.example.sportx.Service.LeaderboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/leaderboard")
@RequiredArgsConstructor
@Validated
@Tag(name = "Leaderboard", description = "Ranking APIs based on Redis ZSet")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping("/spots/heat")
    @Operation(summary = "Spot heat ranking", description = "Get top spots ranked by heat score")
    public Result<List<SpotHeatRankingDto>> topSpotHeat(
            @RequestParam(value = "limit", defaultValue = "10")
            @Min(value = 1, message = "limit最小为1")
            @Max(value = 50, message = "limit最大为50") int limit) {
        return Result.success(leaderboardService.getSpotHeatRanking(limit));
    }

    @GetMapping("/users/score")
    @Operation(summary = "User score ranking", description = "Get top users ranked by challenge score")
    public Result<List<UserScoreRankingDto>> topUserScore(
            @RequestParam(value = "limit", defaultValue = "10")
            @Min(value = 1, message = "limit最小为1")
            @Max(value = 50, message = "limit最大为50") int limit) {
        return Result.success(leaderboardService.getUserScoreRanking(limit));
    }
}
