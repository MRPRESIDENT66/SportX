package com.example.sportx.Entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("outbox_event")
public class OutboxEvent {

    public static final String STATUS_PENDING   = "PENDING";
    public static final String STATUS_DELIVERED = "DELIVERED";
    public static final String STATUS_FAILED    = "FAILED";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules();   // 支持 LocalDateTime 序列化

    private Long id;
    private String eventType;
    private String payload;
    private String status;
    private Integer retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime deliveredAt;

    /**
     * 从 ChallengeEvent 构建一条 PENDING 的 outbox 记录。
     * id 由调用方（RedisIdGenerator）注入，保持全局唯一。
     */
    public static OutboxEvent of(long id, ChallengeEvent event) {
        OutboxEvent record = new OutboxEvent();
        record.setId(id);
        record.setEventType(event.getEventType().name());
        record.setPayload(serialize(event));
        record.setStatus(STATUS_PENDING);
        record.setRetryCount(0);
        return record;
    }

    /** 将 payload JSON 反序列化回 ChallengeEvent。 */
    public ChallengeEvent toChallengeEvent() {
        try {
            return MAPPER.readValue(payload, ChallengeEvent.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize outbox payload: " + payload, e);
        }
    }

    private static String serialize(ChallengeEvent event) {
        try {
            return MAPPER.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize ChallengeEvent for outbox", e);
        }
    }
}
