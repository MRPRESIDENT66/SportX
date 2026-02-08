package com.example.sportx.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.sportx.Entity.ChallengeParticipation;
import com.example.sportx.Entity.Result;

public interface ChallengeParticipationService extends IService<ChallengeParticipation> {
    Result joinChallenge(Long id);
}
