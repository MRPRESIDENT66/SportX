package com.example.sportx.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.sportx.Entity.Challenge;
import com.example.sportx.Entity.Result;

public interface IChallengeService extends IService<Challenge> {
    void addChallenge(Challenge challenge);
}
