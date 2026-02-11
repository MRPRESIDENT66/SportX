package com.example.sportx.Controller;

import com.example.sportx.Entity.Challenge;
import com.example.sportx.Entity.ChallengeParticipation;
import com.example.sportx.Entity.dto.ChallengeListQueryDto;
import com.example.sportx.Entity.vo.PageResult;
import com.example.sportx.Entity.vo.Result;
import com.example.sportx.Service.ChallengeParticipationService;
import com.example.sportx.Service.ChallengeService;
import com.example.sportx.Utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/challenge")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challengeService;
    private final ChallengeParticipationService challengeParticipationService;

    @GetMapping("/list")
    public Result<PageResult<Challenge>> listChallenges(ChallengeListQueryDto queryDto) {
        return challengeService.listChallenges(queryDto);
    }

    @GetMapping("/{id}")
    public Result<Challenge> getChallengeDetail(@PathVariable("id") Long challengeId) {
        return challengeService.getChallengeDetail(challengeId);
    }

    @GetMapping("/my")
    public Result<PageResult<ChallengeParticipation>> myChallenges(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        String userId = UserHolder.getUser().getId();
        return challengeParticipationService.listMyChallenges(userId, page, size);
    }

    @PostMapping("/register/{id}")
    public Result<Long> joinChallenge(@PathVariable("id") Long challengeId) {
        return challengeParticipationService.joinChallenge(challengeId);
    }

    @PostMapping("/cancel/{id}")
    public Result<Void> cancelChallenge(@PathVariable("id") Long challengeId) {
        return challengeParticipationService.cancelChallenge(challengeId);
    }

    @PostMapping("/add")
    public Result<Void> addChallenge(@RequestBody Challenge challenge) {
        challengeService.addChallenge(challenge);
        return Result.success();
    }
}
