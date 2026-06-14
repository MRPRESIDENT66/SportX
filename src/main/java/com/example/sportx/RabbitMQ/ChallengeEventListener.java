package com.example.sportx.RabbitMQ;

import com.example.sportx.Entity.ChallengeEvent;
import com.example.sportx.Service.FailedMessageService;
import com.example.sportx.Service.LeaderboardService;
import com.example.sportx.Service.NotificationService;
import com.example.sportx.Utils.NotificationKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.example.sportx.RabbitMQ.RabbitConstants.CHALLENGE_EVENT_DLX_QUEUE;
import static com.example.sportx.RabbitMQ.RabbitConstants.CHALLENGE_EVENT_QUEUE;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChallengeEventListener {

    private static final double SIGNUP_SCORE_DELTA = 10D;
    private static final double CANCEL_SCORE_DELTA = -10D;

    private final NotificationService notificationService;
    private final StringRedisTemplate redisTemplate;
    private final ChallengeEventScheduler scheduler;
    private final LeaderboardService leaderboardService;
    private final FailedMessageService failedMessageService;

    @RabbitListener(queues = CHALLENGE_EVENT_QUEUE)
    public void onChallengeEvent(@Payload ChallengeEvent event) {
        if (event == null || event.getEventType() == null) {
            log.warn("Received malformed challenge event: {}", event);
            return;
        }

        // 未来事件先挂调度器，等到触发时间再投递。
        if (isFutureEvent(event)) {
            scheduler.schedule(event);
            log.info("Scheduled future challenge event: {}", event);
            return;
        }

        // SETNX 原子抢占幂等 key：抢到 = 首次处理，抢不到 = 已处理或正在处理，直接 ack 跳过。
        // 与旧版 hasKey→处理→set 的"查-改"两步不同，setIfAbsent 是单条原子命令，
        // 多个重复消息并发到达时只有一个能抢到，从根本上消除并发重复执行的窗口。
        String idempotencyKey = buildIdempotencyKey(event);
        boolean claimed = Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(idempotencyKey, "1", 7, TimeUnit.DAYS)
        );
        if (!claimed) {
            log.debug("Challenge event already claimed, skipping: {}", idempotencyKey);
            return;
        }

        try {
            dispatch(event);
        } catch (Exception e) {
            // 业务异常：释放幂等 key，让 RabbitMQ 重试机制重新投递。
            // 若不释放，重试时会被幂等 key 拦截，永远不会执行，最终静默丢消息。
            redisTemplate.delete(idempotencyKey);
            log.error("Challenge event consume failed, releasing idempotency key for retry. event={}", event, e);
            throw e;
        }
    }

    @RabbitListener(queues = CHALLENGE_EVENT_DLX_QUEUE)
    public void onDeadLetter(@Payload Message message) {
        String payload = message == null ? null : new String(message.getBody());
        failedMessageService.recordDeadLetter(message, "message moved to DLQ after retry exhausted");
        log.error("Challenge event moved to DLQ and persisted. payload={}", payload);
    }

    private void dispatch(ChallengeEvent event) {
        String eventType = event.getEventType().name();
        switch (event.getEventType()) {
            case SIGN_UP_SUCCESS:
                notificationService.notifySignupSuccess(event);
                // incrementSpotHeat / incrementUserScore 内部先写 DB 流水（唯一约束）再 ZINCRBY，
                // 保证即使此处被重复调用，ZSet 也不会重复累加。
                leaderboardService.incrementSpotHeat(
                        event.getSpotId(), event.getUserId(), event.getChallengeId(), eventType, 1D);
                leaderboardService.incrementUserScore(
                        event.getUserId(), event.getChallengeId(), eventType, SIGNUP_SCORE_DELTA);
                break;
            case CANCEL_SUCCESS:
                notificationService.notifyCancelSuccess(event);
                leaderboardService.incrementSpotHeat(
                        event.getSpotId(), event.getUserId(), event.getChallengeId(), eventType, -1D);
                leaderboardService.incrementUserScore(
                        event.getUserId(), event.getChallengeId(), eventType, CANCEL_SCORE_DELTA);
                break;
            case START_REMINDER:
                notificationService.notifyStartReminder(event);
                break;
            case END_REMINDER:
                notificationService.notifyEndReminder(event);
                break;
            default:
                log.warn("Unsupported challenge event type: {}", event.getEventType());
        }
    }

    private boolean isFutureEvent(ChallengeEvent event) {
        LocalDateTime trigger = event.getTriggerTime();
        return trigger != null && trigger.isAfter(LocalDateTime.now().plusHours(1));
    }

    private String buildIdempotencyKey(ChallengeEvent event) {
        return NotificationKeys.statusKey(
                Objects.requireNonNullElse(event.getChallengeId(), 0L),
                event.getEventType().name(),
                Objects.requireNonNullElse(event.getUserId(), "_ALL_")
        );
    }
}
