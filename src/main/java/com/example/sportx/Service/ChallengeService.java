package com.example.sportx.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.sportx.Entity.Challenge;
import com.example.sportx.Entity.dto.ChallengeListQueryDto;
import com.example.sportx.Entity.vo.PageResult;
import com.example.sportx.Entity.vo.Result;

public interface ChallengeService extends IService<Challenge> {
    void addChallenge(Challenge challenge);

    Result<Void> updateChallenge(Challenge challenge);

    Result<Void> deleteChallenge(Long challengeId);

    Result<PageResult<Challenge>> listChallenges(ChallengeListQueryDto queryDto);

    Result<Challenge> getChallengeDetail(Long challengeId);

}
