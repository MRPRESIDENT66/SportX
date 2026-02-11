package com.example.sportx.Entity;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
public class ChallengeEvent implements Serializable {

    public enum EventType {
        SIGN_UP_SUCCESS,
        CANCEL_SUCCESS,
        START_REMINDER,
        END_REMINDER
    }

    private EventType eventType;
    private Long challengeId;
    private String userId;
    private Long spotId;
    private LocalDateTime triggerTime;
    private String channel;
    private String message;
}
