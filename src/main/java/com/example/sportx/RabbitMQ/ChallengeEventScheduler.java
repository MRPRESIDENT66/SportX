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
            // 以 JSON 作为 ZSet member，score 使用触发时间秒级时间戳。
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
        // 每分钟扫描一次“到期消息”，避免未来消息过早进入消费链路。
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
                // 发布成功后再从调度集合移除，降低消息丢失风险。
                redisTemplate.opsForZSet().remove(NotificationKeys.scheduledSetKey(), payload);
            } catch (JsonProcessingException e) {
                // 反序列化失败保留原数据，便于后续人工排查。
                log.error("Failed to parse scheduled event", e);
            }
        }
    }
}
