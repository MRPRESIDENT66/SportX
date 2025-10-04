package com.example.sportx.RabbitMQ;

import com.example.sportx.Entity.ChallengeEvent;
import com.example.sportx.Utils.NotificationKeys;
import com.example.sportx.Utils.RabbitMqHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChallengeEventScheduler {

    private final StringRedisTemplate redisTemplate;
    private final RabbitMqHelper rabbitMqHelper;
    private final ObjectMapper objectMapper;

    public void schedule(ChallengeEvent event) {
        if (event == null || event.getTriggerTime() == null) {
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(event);
            long epochSecond = event.getTriggerTime().atZone(ZoneId.systemDefault()).toEpochSecond();
            redisTemplate.opsForZSet().add(NotificationKeys.scheduledSetKey(), payload, epochSecond);
            log.info("Queued challenge event for delayed dispatch: {}", event);
        } catch (JsonProcessingException e) {
            log.error("Failed to schedule challenge event", e);
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void dispatchDueEvents() {
        long now = Instant.now().getEpochSecond();
        Set<String> payloads = redisTemplate.opsForZSet().rangeByScore(NotificationKeys.scheduledSetKey(), 0, now);
        if (payloads == null || payloads.isEmpty()) {
            return;
        }
        for (String payload : payloads) {
            try {
                ChallengeEvent event = objectMapper.readValue(payload, ChallengeEvent.class);
                rabbitMqHelper.publishChallengeEvent(event);
                log.info("Dispatched scheduled challenge event: {}", event);
                redisTemplate.opsForZSet().remove(NotificationKeys.scheduledSetKey(), payload);
            } catch (JsonProcessingException e) {
                log.error("Failed to parse scheduled event", e);
            }
        }
    }
}
