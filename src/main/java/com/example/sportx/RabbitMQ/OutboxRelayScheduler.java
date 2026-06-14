package com.example.sportx.RabbitMQ;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.sportx.Entity.ChallengeEvent;
import com.example.sportx.Entity.OutboxEvent;
import com.example.sportx.Mapper.OutboxEventMapper;
import com.example.sportx.Utils.RabbitMqHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.example.sportx.Utils.RedisConstants.CACHE_CHALLENGE_KEY;

/**
 * Outbox relay：轮询 outbox_event 表中 PENDING 记录，投递到 RabbitMQ 后标记 DELIVERED。
 *
 * 职责边界：
 *  - 它是整个系统中唯一调用 rabbitMqHelper.publishChallengeEvent 的地方（除定时提醒外）。
 *  - 业务层（join/cancel）只负责在同一事务内写 outbox 记录，不感知 MQ 的存在。
 *
 * 可靠性保证：
 *  - outbox 记录与业务写同事务提交，COMMIT 后才对 relay 可见，彻底消除"提交前发消息"。
 *  - 即使应用在 COMMIT 后宕机，relay 重启后仍能从表中恢复 PENDING 记录继续投递，不丢事件。
 *  - 消费端已有 Redis 幂等标记，relay 的"至少一次投递"不会导致业务重复执行。
 *
 * 并发安全：
 *  - 多实例部署时，通过 Redis SETNX 抢占全局锁，只有一个实例在同一轮次执行 relay，
 *    避免同一条 outbox 记录被多个实例同时投递。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelayScheduler {

    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRY = 5;
    private static final String RELAY_LOCK_KEY = "outbox:relay:lock";
    private static final long RELAY_LOCK_TTL_SECONDS = 25;

    private final OutboxEventMapper outboxEventMapper;
    private final RabbitMqHelper rabbitMqHelper;
    private final StringRedisTemplate redisTemplate;

    @Scheduled(fixedDelay = 3000)
    public void relay() {
        // 多实例部署时只让一个节点在同一轮次执行，避免重复投递。
        if (!tryRelayLock()) {
            return;
        }
        try {
            List<OutboxEvent> pending = fetchPending();
            if (pending.isEmpty()) {
                return;
            }
            for (OutboxEvent record : pending) {
                process(record);
            }
        } finally {
            redisTemplate.delete(RELAY_LOCK_KEY);
        }
    }

    private List<OutboxEvent> fetchPending() {
        LambdaQueryWrapper<OutboxEvent> qw = new LambdaQueryWrapper<OutboxEvent>()
                .eq(OutboxEvent::getStatus, OutboxEvent.STATUS_PENDING)
                .lt(OutboxEvent::getRetryCount, MAX_RETRY)
                .orderByAsc(OutboxEvent::getCreatedAt)
                .last("LIMIT " + BATCH_SIZE);
        return outboxEventMapper.selectList(qw);
    }

    private void process(OutboxEvent record) {
        try {
            ChallengeEvent event = record.toChallengeEvent();

            // 先删缓存，再投递 MQ：确保消费端处理通知/积分时，缓存已是最新。
            evictChallengeCache(event.getChallengeId());
            rabbitMqHelper.publishChallengeEvent(event);

            markDelivered(record.getId());
            log.debug("Outbox relay delivered: id={} type={}", record.getId(), record.getEventType());
        } catch (Exception e) {
            incrementRetry(record);
            log.error("Outbox relay failed: id={} retry={}", record.getId(), record.getRetryCount() + 1, e);
        }
    }

    private void markDelivered(Long id) {
        LambdaUpdateWrapper<OutboxEvent> uw = new LambdaUpdateWrapper<OutboxEvent>()
                .eq(OutboxEvent::getId, id)
                .set(OutboxEvent::getStatus, OutboxEvent.STATUS_DELIVERED)
                .set(OutboxEvent::getDeliveredAt, LocalDateTime.now());
        outboxEventMapper.update(null, uw);
    }

    private void incrementRetry(OutboxEvent record) {
        int next = record.getRetryCount() + 1;
        LambdaUpdateWrapper<OutboxEvent> uw = new LambdaUpdateWrapper<OutboxEvent>()
                .eq(OutboxEvent::getId, record.getId())
                .set(OutboxEvent::getRetryCount, next)
                .set(next >= MAX_RETRY, OutboxEvent::getStatus, OutboxEvent.STATUS_FAILED);
        outboxEventMapper.update(null, uw);
    }

    private void evictChallengeCache(Long challengeId) {
        if (challengeId == null) {
            return;
        }
        redisTemplate.delete(CACHE_CHALLENGE_KEY + challengeId);
    }

    private boolean tryRelayLock() {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(RELAY_LOCK_KEY, "1", RELAY_LOCK_TTL_SECONDS, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(acquired);
    }
}
