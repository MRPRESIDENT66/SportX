package com.example.sportx.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 场馆 ES 索引同步事件，由 OutboxRelay 投递到 search-sync 队列，SpotIndexListener 消费。
 *
 * <p>只携带 spotId 与操作类型，不带完整数据：消费端用 spotId 回 DB 读最新值，
 * 天然幂等且最终一致——即使消息重复或乱序，结果都收敛到 DB 当前状态。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpotSyncEvent implements Serializable {

    /** {@link OutboxEvent#EVENT_SPOT_UPSERT} 或 {@link OutboxEvent#EVENT_SPOT_DELETE}。 */
    private String eventType;
    private Long spotId;
}
