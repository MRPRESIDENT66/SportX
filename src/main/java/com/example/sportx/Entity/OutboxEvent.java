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

    // 搜索同步事件类型：relay 按 eventType 区分这类记录，路由到 search-sync 队列而非业务队列。
    public static final String EVENT_SPOT_UPSERT = "SPOT_UPSERT";
    public static final String EVENT_SPOT_DELETE = "SPOT_DELETE";

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

    /**
     * 构建一条场馆 ES 同步的 outbox 记录。payload 只存 spotId，
     * 消费端用它回 DB 读最新数据，保证幂等与最终一致。
     */
    public static OutboxEvent ofSpotSync(long id, String eventType, Long spotId) {
        OutboxEvent record = new OutboxEvent();
        record.setId(id);
        record.setEventType(eventType);
        record.setPayload(String.valueOf(spotId));
        record.setStatus(STATUS_PENDING);
        record.setRetryCount(0);
        return record;
    }

    /** 是否为搜索同步类事件（relay 据此决定路由到 search-sync 队列）。 */
    public boolean isSpotSyncEvent() {
        return EVENT_SPOT_UPSERT.equals(eventType) || EVENT_SPOT_DELETE.equals(eventType);
    }

    /** 从 payload 取出 spotId（仅对 spot 同步事件有效）。 */
    public Long spotIdFromPayload() {
        return Long.valueOf(payload);
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
