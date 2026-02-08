package com.example.sportx.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.sportx.Entity.ChallengeParticipation;
import com.example.sportx.Entity.vo.Result;

public interface ChallengeParticipationService extends IService<ChallengeParticipation> {
    Result<Long> joinChallenge(Long id);
}
