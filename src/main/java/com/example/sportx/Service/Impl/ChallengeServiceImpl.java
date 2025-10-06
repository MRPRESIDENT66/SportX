package com.example.sportx.Service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.sportx.Entity.Challenge;
import com.example.sportx.Entity.ChallengeEvent;
import com.example.sportx.Mapper.ChallengeMapper;
import com.example.sportx.Service.IChallengeService;
import com.example.sportx.Utils.RabbitMqHelper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ChallengeServiceImpl extends ServiceImpl<ChallengeMapper, Challenge> implements IChallengeService {


    @Resource
    private RabbitMqHelper rabbitMqHelper;

    @Transactional
    @Override
    public void addChallenge(Challenge challenge) {
        save(challenge);
        scheduleReminders(challenge);
    }

    private void scheduleReminders(Challenge challenge) {
        if (challenge == null || challenge.getId() == null) {
            return;
        }
        if (challenge.getStartTime() != null) {
            ChallengeEvent startEvent = ChallengeEvent.builder()
                    .eventType(ChallengeEvent.EventType.START_REMINDER)
                    .challengeId(challenge.getId())
                    .spotId(challenge.getSpotId())
                    .triggerTime(challenge.getStartTime().atStartOfDay())
                    .build();
            rabbitMqHelper.publishChallengeEvent(startEvent);
        }
        if (challenge.getEndTime() != null) {
            ChallengeEvent endEvent = ChallengeEvent.builder()
                    .eventType(ChallengeEvent.EventType.END_REMINDER)
                    .challengeId(challenge.getId())
                    .spotId(challenge.getSpotId())
                    .triggerTime(challenge.getEndTime().atStartOfDay())
                    .build();
            rabbitMqHelper.publishChallengeEvent(endEvent);
        }
    }
}
