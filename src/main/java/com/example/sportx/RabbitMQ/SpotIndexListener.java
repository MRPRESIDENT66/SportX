package com.example.sportx.RabbitMQ;

import com.example.sportx.Entity.OutboxEvent;
import com.example.sportx.Entity.Spots;
import com.example.sportx.Entity.SpotSyncEvent;
import com.example.sportx.Search.SpotDocument;
import com.example.sportx.Search.SpotSearchRepository;
import com.example.sportx.Service.SpotsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import static com.example.sportx.RabbitMQ.RabbitConstants.SEARCH_SYNC_DLX_QUEUE;
import static com.example.sportx.RabbitMQ.RabbitConstants.SEARCH_SYNC_QUEUE;

/**
 * 场馆 ES 索引同步消费者：把 MySQL 的场馆变更落到 Elasticsearch。
 *
 * <p>幂等设计：消费时只用 spotId 回 DB 读最新数据再写 ES，不信任消息内容。
 * ES 的 index（按 _id upsert）与 delete 操作本身幂等，配合 read-after-write，
 * 即使消息重复投递或乱序到达，最终都收敛到 DB 当前状态——无需额外幂等表。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpotIndexListener {

    private final SpotsService spotsService;
    private final SpotSearchRepository spotSearchRepository;

    @RabbitListener(queues = SEARCH_SYNC_QUEUE)
    public void onSpotSync(@Payload SpotSyncEvent event) {
        if (event == null || event.getSpotId() == null || event.getEventType() == null) {
            log.warn("Received malformed spot sync event: {}", event);
            return;
        }
        switch (event.getEventType()) {
            case OutboxEvent.EVENT_SPOT_UPSERT -> upsert(event.getSpotId());
            case OutboxEvent.EVENT_SPOT_DELETE -> spotSearchRepository.deleteById(event.getSpotId());
            default -> log.warn("Unsupported spot sync event type: {}", event.getEventType());
        }
    }

    @RabbitListener(queues = SEARCH_SYNC_DLX_QUEUE)
    public void onDeadLetter(@Payload Message message) {
        String payload = message == null ? null : new String(message.getBody());
        log.error("Spot sync event moved to DLQ after retry exhausted. payload={}", payload);
    }

    private void upsert(Long spotId) {
        Spots spot = spotsService.getById(spotId);
        if (spot == null) {
            // DB 中已不存在：保证最终一致，从 ES 也移除。
            spotSearchRepository.deleteById(spotId);
            return;
        }
        spotSearchRepository.save(SpotDocument.from(spot));
        log.debug("Synced spot to ES: id={}", spotId);
    }
}
