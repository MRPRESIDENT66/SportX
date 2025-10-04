package com.example.sportx.RabbitMQ;

import com.example.sportx.Entity.ChallengeEvent;
import com.example.sportx.Service.NotificationService;
import com.example.sportx.Utils.NotificationKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.example.sportx.RabbitMQ.RabbitConstants.CHALLENGE_EVENT_QUEUE;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChallengeEventListener {

    private final NotificationService notificationService;
    private final StringRedisTemplate redisTemplate;
    private final ChallengeEventScheduler scheduler;

    @RabbitListener(queues = CHALLENGE_EVENT_QUEUE)
    public void onChallengeEvent(@Payload ChallengeEvent event) {
        if (event == null || event.getEventType() == null) {
            log.warn("Received malformed challenge event: {}", event);
            return;
        }
        if (!shouldDeliver(event)) {
            return;
        }
        switch (event.getEventType()) {
            case SIGN_UP_SUCCESS:
                notificationService.notifySignupSuccess(event);
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
