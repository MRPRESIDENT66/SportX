package com.example.sportx.Controller;

import com.example.sportx.Entity.Challenge;
import com.example.sportx.Entity.ChallengeParticipation;
import com.example.sportx.Entity.dto.ChallengeListQueryDto;
import com.example.sportx.Entity.vo.PageResult;
import com.example.sportx.Entity.vo.Result;
import com.example.sportx.Service.ChallengeParticipationService;
import com.example.sportx.Service.ChallengeService;
import com.example.sportx.Utils.UserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/challenge")
@RequiredArgsConstructor
@Validated
@Tag(name = "Challenge", description = "Challenge lifecycle and participation APIs")
public class ChallengeController {

    private final ChallengeService challengeService;
    private final ChallengeParticipationService challengeParticipationService;

    @GetMapping("/list")
    @Operation(summary = "List challenges", description = "Get paged challenges with optional filters")
    public Result<PageResult<Challenge>> listChallenges(@Valid ChallengeListQueryDto queryDto) {
        // 挑战分页列表：支持状态过滤（upcoming/ongoing/ended）与关键词检索。
        return challengeService.listChallenges(queryDto);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Challenge detail", description = "Get challenge detail by id")
    public Result<Challenge> getChallengeDetail(@PathVariable("id") @Positive(message = "挑战ID必须大于0") Long challengeId) {
        // 挑战详情：服务层会优先走缓存。
        return challengeService.getChallengeDetail(challengeId);
    }

    @GetMapping("/my")
    @Operation(summary = "My participations", description = "Get paged participation records for current user")
    public Result<PageResult<ChallengeParticipation>> myChallenges(
            @RequestParam(value = "page", defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") @Min(value = 1, message = "每页最少1条") Integer size) {
        // 当前用户报名记录分页查询。
        String userId = UserHolder.getUser().getId();
        return challengeParticipationService.listMyChallenges(userId, page, size);
    }

    @PostMapping("/register/{id}")
    @Operation(summary = "Register challenge", description = "Register current user to a challenge")
    public Result<Long> joinChallenge(@PathVariable("id") @Positive(message = "挑战ID必须大于0") Long challengeId) {
        // 报名挑战：含分布式锁、防重、名额扣减与事件发布。
        return challengeParticipationService.joinChallenge(challengeId);
    }

    @PostMapping("/cancel/{id}")
    @Operation(summary = "Cancel registration", description = "Cancel current user's participation and release slot")
    public Result<Void> cancelChallenge(@PathVariable("id") @Positive(message = "挑战ID必须大于0") Long challengeId) {
        // 取消报名：回收名额并发布取消事件。
        return challengeParticipationService.cancelChallenge(challengeId);
    }

    @PostMapping("/add")
    @Operation(summary = "Create challenge", description = "Create a challenge and schedule reminder events")
    public Result<Void> addChallenge(@RequestBody Challenge challenge) {
        // 新建挑战并注册开赛/结束提醒事件。
        challengeService.addChallenge(challenge);
        return Result.success();
    }
}
