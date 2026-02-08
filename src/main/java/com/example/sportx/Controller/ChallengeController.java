package com.example.sportx.Controller;

import com.example.sportx.Entity.Challenge;
import com.example.sportx.Entity.Result;
import com.example.sportx.Service.ChallengeParticipationService;
import com.example.sportx.Service.ChallengeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/challenge")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challengeService;
    private final ChallengeParticipationService challengeParticipationService;

    @PostMapping("/register/{id}")
    public Result joinChallenge(@PathVariable("id")Long challengeId){
        return challengeParticipationService.joinChallenge(challengeId);
    }

    @PostMapping("/add")
    public void addChallenge(@RequestBody Challenge challenge){
        challengeService.addChallenge(challenge);
    }
}
