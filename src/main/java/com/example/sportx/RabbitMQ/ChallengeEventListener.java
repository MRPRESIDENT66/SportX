package com.example.sportx.RabbitMQ;

import com.example.sportx.Entity.ChallengeEvent;
import com.example.sportx.Service.LeaderboardService;
import com.example.sportx.Service.FailedMessageService;
import com.example.sportx.Service.NotificationService;
import com.example.sportx.Utils.NotificationKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.example.sportx.RabbitMQ.RabbitConstants.CHALLENGE_EVENT_QUEUE;
import static com.example.sportx.RabbitMQ.RabbitConstants.CHALLENGE_EVENT_DLX_QUEUE;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChallengeEventListener {
    private static final double SIGNUP_SCORE_DELTA = 10D;
    private static final double CANCEL_SCORE_DELTA = -10D;

    private final NotificationService notificationService;
    private final StringRedisTemplate redisTemplate;
    private final ChallengeEventScheduler scheduler;
    private final LeaderboardService leaderboardService;
    private final FailedMessageService failedMessageService;

    @RabbitListener(queues = CHALLENGE_EVENT_QUEUE)
    public void onChallengeEvent(@Payload ChallengeEvent event) {
        if (event == null || event.getEventType() == null) {
            log.warn("Received malformed challenge event: {}", event);
            return;
        }
        if (!shouldDeliver(event)) {
            return;
        }
        try {
            switch (event.getEventType()) {
                case SIGN_UP_SUCCESS:
                    notificationService.notifySignupSuccess(event);
                    leaderboardService.incrementSpotHeat(event.getSpotId(), 1D);
                    leaderboardService.incrementUserScore(event.getUserId(), SIGNUP_SCORE_DELTA);
                    break;
                case CANCEL_SUCCESS:
                    notificationService.notifyCancelSuccess(event);
                    leaderboardService.incrementSpotHeat(event.getSpotId(), -1D);
                    leaderboardService.incrementUserScore(event.getUserId(), CANCEL_SCORE_DELTA);
                    break;
                case START_REMINDER:
                    notificationService.notifyStartReminder(event);
                    break;
                case END_REMINDER:
                    notificationService.notifyEndReminder(event);
                    break;
                default:
                    log.warn("Unsupported challenge event type: {}", event.getEventType());
            }
            markDelivered(event);
        } catch (Exception exception) {
            log.error("Challenge event consume failed, will retry or dead-letter. event={}", event, exception);
            throw exception;
        }
    }

    @RabbitListener(queues = CHALLENGE_EVENT_DLX_QUEUE)
    public void onDeadLetter(@Payload Message message) {
        String payload = message == null ? null : new String(message.getBody());
        String reason = "message moved to DLQ after retry exhausted";
        failedMessageService.recordDeadLetter(message, reason);
        log.error("Challenge event moved to DLQ and persisted. payload={}", payload);
    }

    private boolean shouldDeliver(ChallengeEvent event) {
        String key = NotificationKeys.statusKey(
                Objects.requireNonNullElse(event.getChallengeId(), 0L),
                event.getEventType().name(),
                Objects.requireNonNullElse(event.getUserId(), "_ALL_")
        );
        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(exists)) {
            log.debug("Challenge event already processed: {}", key);
            return false;
        }
        LocalDateTime trigger = event.getTriggerTime();
        if (trigger != null && trigger.isAfter(LocalDateTime.now().plusHours(1))) {
            scheduler.schedule(event);
            log.info("Scheduled future challenge event: {}", event);
            return false;
        }
        return true;
    }

    private void markDelivered(ChallengeEvent event) {
        String key = NotificationKeys.statusKey(
                Objects.requireNonNullElse(event.getChallengeId(), 0L),
                event.getEventType().name(),
                Objects.requireNonNullElse(event.getUserId(), "_ALL_")
        );
        long ttl = TimeUnit.DAYS.toSeconds(7);
        redisTemplate.opsForValue().set(key, LocalDateTime.now().toString(), ttl, TimeUnit.SECONDS);
    }
}
