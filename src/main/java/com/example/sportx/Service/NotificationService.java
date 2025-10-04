package com.example.sportx.Service;

import com.example.sportx.Entity.ChallengeEvent;

public interface NotificationService {

    void notifySignupSuccess(ChallengeEvent event);

    void notifyStartReminder(ChallengeEvent event);

    void notifyEndReminder(ChallengeEvent event);
}
