package com.example.sportx.Controller;

import com.example.sportx.Entity.Challenge;
import com.example.sportx.Entity.Result;
import com.example.sportx.Service.Impl.ChallengeParServiceImpl;
import com.example.sportx.Service.Impl.ChallengeServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/challenge")
public class ChallengeController {

    @Resource
    ChallengeServiceImpl challengeService;
    ChallengeParServiceImpl challengeParService;

    @PostMapping("register/{id}")
    public Result joinChallenge(@PathVariable("id")Long ChallengeId){
        return challengeParService.joinChallenge(ChallengeId);
    }

    @GetMapping("/add")
    public void addChallenge(Challenge challenge){
        challengeService.addChallenge(challenge);
    }
}
