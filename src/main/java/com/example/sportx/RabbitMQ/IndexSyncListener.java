package com.example.sportx.RabbitMQ;

import com.example.sportx.Entity.Challenge;
import com.example.sportx.Entity.IndexSyncEvent;
import com.example.sportx.Entity.OutboxEvent;
import com.example.sportx.Entity.Spots;
import com.example.sportx.Search.ChallengeDocument;
import com.example.sportx.Search.ChallengeSearchRepository;
import com.example.sportx.Search.SpotDocument;
import com.example.sportx.Search.SpotSearchRepository;
import com.example.sportx.Service.ChallengeService;
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
 * 统一的 ES 索引同步消费者：把 MySQL 的 spot / challenge 变更落到 Elasticsearch。
 *
 * <p>所有索引同步事件共用一条 search-sync 队列，本消费者按 {@link IndexSyncEvent#getAggregate()}
 * 分发到对应索引——新增可搜索资源只需在此加一个分支，无需新建队列与监听器。
 *
 * <p>幂等设计：消费时只用 id 回 DB 读最新数据再写 ES，不信任消息内容。
 * ES 的 index（按 _id upsert）与 delete 操作本身幂等，配合 read-after-write，
 * 即使消息重复投递或乱序到达，最终都收敛到 DB 当前状态——无需额外幂等表。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IndexSyncListener {

    private final SpotsService spotsService;
    private final SpotSearchRepository spotSearchRepository;
    private final ChallengeService challengeService;
    private final ChallengeSearchRepository challengeSearchRepository;

    @RabbitListener(queues = SEARCH_SYNC_QUEUE)
    public void onIndexSync(@Payload IndexSyncEvent event) {
        if (event == null || event.getId() == null || event.getAggregate() == null || event.getOp() == null) {
            log.warn("Received malformed index sync event: {}", event);
            return;
        }
        switch (event.getAggregate()) {
            case OutboxEvent.AGG_SPOT -> syncSpot(event);
            case OutboxEvent.AGG_CHALLENGE -> syncChallenge(event);
            default -> log.warn("Unsupported index sync aggregate: {}", event.getAggregate());
        }
    }

    @RabbitListener(queues = SEARCH_SYNC_DLX_QUEUE)
    public void onDeadLetter(@Payload Message message) {
        String payload = message == null ? null : new String(message.getBody());
        log.error("Index sync event moved to DLQ after retry exhausted. payload={}", payload);
    }

    private void syncSpot(IndexSyncEvent event) {
        if (OutboxEvent.OP_DELETE.equals(event.getOp())) {
            spotSearchRepository.deleteById(event.getId());
            return;
        }
        Spots spot = spotsService.getById(event.getId());
        if (spot == null) {
            // DB 中已不存在：保证最终一致，从 ES 也移除。
            spotSearchRepository.deleteById(event.getId());
            return;
        }
        spotSearchRepository.save(SpotDocument.from(spot));
        log.debug("Synced spot to ES: id={}", event.getId());
    }

    private void syncChallenge(IndexSyncEvent event) {
        if (OutboxEvent.OP_DELETE.equals(event.getOp())) {
            challengeSearchRepository.deleteById(event.getId());
            return;
        }
        Challenge challenge = challengeService.getById(event.getId());
        if (challenge == null) {
            challengeSearchRepository.deleteById(event.getId());
            return;
        }
        challengeSearchRepository.save(ChallengeDocument.from(challenge));
        log.debug("Synced challenge to ES: id={}", event.getId());
    }
}
