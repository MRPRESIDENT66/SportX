package com.example.sportx.Service.Impl;

import com.example.sportx.Entity.ChallengeEvent;
import com.example.sportx.Service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    @Override
    public void notifySignupSuccess(ChallengeEvent event) {
        log.info("[Notify] signup success -> challenge={}, user={}, trigger={}",
                event.getChallengeId(),
                event.getUserId(),
                event.getTriggerTime());
    }

    @Override
    public void notifyStartReminder(ChallengeEvent event) {
        log.info("[Notify] start reminder -> challenge={}, trigger={}",
                event.getChallengeId(),
                event.getTriggerTime());
    }

    @Override
    public void notifyEndReminder(ChallengeEvent event) {
        log.info("[Notify] end reminder -> challenge={}, trigger={}",
                event.getChallengeId(),
                event.getTriggerTime());
    }
}
