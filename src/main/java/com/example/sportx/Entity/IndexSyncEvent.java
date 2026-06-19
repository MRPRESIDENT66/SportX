package com.example.sportx.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 通用 ES 索引同步事件，承载 spot / challenge 等多种聚合的索引变更。
 *
 * <p>由 OutboxRelay 投递到统一的 search-sync 队列，IndexSyncListener 按 {@link #aggregate}
 * 分发到对应索引。只携带聚合类型、操作和目标 id，不带完整数据：消费端用 id 回 DB 读最新值，
 * 天然幂等且最终一致——即使消息重复或乱序，结果都收敛到 DB 当前状态。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndexSyncEvent implements Serializable {

    /** 聚合类型：{@link OutboxEvent#AGG_SPOT} 或 {@link OutboxEvent#AGG_CHALLENGE}。 */
    private String aggregate;
    /** 操作：{@link OutboxEvent#OP_UPSERT} 或 {@link OutboxEvent#OP_DELETE}。 */
    private String op;
    /** 目标实体 id。 */
    private Long id;

    /**
     * 从 outbox 的 eventType（形如 {@code SPOT_UPSERT} / {@code CHALLENGE_DELETE}）解析出本事件。
     * 约定：最后一个下划线前是聚合类型，之后是操作。
     */
    public static IndexSyncEvent from(String eventType, Long id) {
        int idx = eventType.lastIndexOf('_');
        return new IndexSyncEvent(eventType.substring(0, idx), eventType.substring(idx + 1), id);
    }
}
