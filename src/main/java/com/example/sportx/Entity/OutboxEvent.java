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

    // 索引同步事件：relay 按 eventType 区分这类记录，路由到统一的 search-sync 队列而非业务队列。
    // eventType 命名约定 = {聚合}_{操作}，如 SPOT_UPSERT / CHALLENGE_DELETE。
    public static final String AGG_SPOT = "SPOT";
    public static final String AGG_CHALLENGE = "CHALLENGE";
    public static final String OP_UPSERT = "UPSERT";
    public static final String OP_DELETE = "DELETE";

    public static final String EVENT_SPOT_UPSERT = AGG_SPOT + "_" + OP_UPSERT;
    public static final String EVENT_SPOT_DELETE = AGG_SPOT + "_" + OP_DELETE;
    public static final String EVENT_CHALLENGE_UPSERT = AGG_CHALLENGE + "_" + OP_UPSERT;
    public static final String EVENT_CHALLENGE_DELETE = AGG_CHALLENGE + "_" + OP_DELETE;

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
     * 构建一条 ES 索引同步的 outbox 记录。payload 只存目标实体 id，
     * 消费端用它回 DB 读最新数据，保证幂等与最终一致。
     *
     * @param eventType 形如 SPOT_UPSERT / CHALLENGE_DELETE
     * @param targetId  目标实体 id（spotId 或 challengeId）
     */
    public static OutboxEvent ofIndexSync(long id, String eventType, Long targetId) {
        OutboxEvent record = new OutboxEvent();
        record.setId(id);
        record.setEventType(eventType);
        record.setPayload(String.valueOf(targetId));
        record.setStatus(STATUS_PENDING);
        record.setRetryCount(0);
        return record;
    }

    /** 是否为索引同步类事件（relay 据此决定路由到 search-sync 队列）。 */
    public boolean isIndexSyncEvent() {
        return eventType != null
                && (eventType.startsWith(AGG_SPOT + "_") || eventType.startsWith(AGG_CHALLENGE + "_"))
                && (eventType.endsWith("_" + OP_UPSERT) || eventType.endsWith("_" + OP_DELETE));
    }

    /** 从 payload 取出目标实体 id（仅对索引同步事件有效）。 */
    public Long targetIdFromPayload() {
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
