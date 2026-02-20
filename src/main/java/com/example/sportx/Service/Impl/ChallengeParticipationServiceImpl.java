package com.example.sportx.Service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.sportx.Entity.Challenge;
import com.example.sportx.Entity.ChallengeEvent;
import com.example.sportx.Entity.ChallengeParticipation;
import com.example.sportx.Entity.User;
import com.example.sportx.Entity.vo.PageResult;
import com.example.sportx.Entity.vo.Result;
import com.example.sportx.Mapper.ChallengeParMapper;
import com.example.sportx.Service.ChallengeParticipationService;
import com.example.sportx.Service.ChallengeService;
import com.example.sportx.Utils.RabbitMqHelper;
import com.example.sportx.Utils.RedisIDWorker;
import com.example.sportx.Utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.example.sportx.Utils.RedisConstants.CACHE_CHALLENGE_KEY;
import static com.example.sportx.Utils.RedisConstants.LOCK_CHALLENGE_JOIN_KEY;

@Service
@RequiredArgsConstructor
public class ChallengeParticipationServiceImpl extends ServiceImpl<ChallengeParMapper, ChallengeParticipation> implements ChallengeParticipationService {
    // Lua 原子解锁脚本：仅当锁归属匹配时才删除，避免误删他人锁。
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    // 应用级随机前缀，结合线程ID组成锁 owner，区分不同请求来源。
    private static final String LOCK_VALUE_PREFIX = UUID.randomUUID().toString().replace("-", "");

    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "return redis.call('del', KEYS[1]) " +
                "else return 0 end"
        );
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    private final ChallengeService challengeService;
    private final RedisIDWorker redisIDWorker;
    private final RabbitMqHelper rabbitMqHelper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional
    public Result<Long> joinChallenge(Long challengeId) {
        // 1) 基础业务校验：活动存在、时间窗口、名额。
        Challenge challenge = challengeService.getById(challengeId);
        if (challenge == null) {
            return Result.error("活动不存在！");
        }

        LocalDate today = LocalDate.now();
        if (challenge.getStartTime().isAfter(today)) {
            return Result.error("活动报名还未开始！");
        }
        if (challenge.getEndTime().isBefore(today)) {
            return Result.error("活动报名已经结束！");
        }
        if (challenge.getJoinedSlots() >= challenge.getTotalSlots()) {
            return Result.error("活动名额不足！");
        }

        // 2) 当前登录用户校验与用户ID转换。
        User currentUser = UserHolder.getUser();
        if (currentUser == null || currentUser.getId() == null) {
            return Result.error("用户未登录！");
        }
        String userIdStr = currentUser.getId();
        Long userId = parseUserId(userIdStr);
        if (userId == null) {
            return Result.error("用户ID格式错误！");
        }

        // 3) Redis 分布式锁：锁粒度 = 用户 + 挑战，防并发重复报名。
        String lockKey = buildJoinLockKey(challengeId, userId);
        String lockOwner = buildLockOwner();
        boolean locked = tryJoinLock(lockKey, lockOwner);
        if (!locked) {
            return Result.error("请求过于频繁，请稍后重试");
        }
        try {
            // 4) 业务幂等：同一用户同一挑战只允许一条报名记录。
            long count = lambdaQuery()
                    .eq(ChallengeParticipation::getUserId, userId)
                    .eq(ChallengeParticipation::getChallengeId, challengeId)
                    .count();
            if (count > 0) {
                return Result.error("该用户已经下单！");
            }

            // 5) CAS 扣减名额：避免并发下超卖（基于 joinedSlots 条件更新）。
            boolean success = challengeService.update()
                    .setSql("joinedSlots = joinedSlots +1")
                    .eq("id", challengeId)
                    .eq("joinedSlots", challenge.getJoinedSlots())
                    .update();
            if (!success) {
                return Result.error("活动名额不足！");
            }
            // 报名成功后失效挑战详情缓存，避免名额字段脏读。
            evictChallengeCache(challengeId);

            ChallengeParticipation challengeParticipation = buildParticipation(challengeId, challenge.getSpotId(), userId);
            long orderId = redisIDWorker.nextID("order");
            challengeParticipation.setId(orderId);
            try {
                save(challengeParticipation);
            } catch (DuplicateKeyException duplicateKeyException) {
                // 数据库唯一索引兜底，防止极端并发绕过上层检查。
                throw new IllegalArgumentException("你已报名该挑战，请勿重复操作");
            }

            // 6) 报名成功后发布事件，交给 MQ 异步做通知/榜单等后续动作。
            ChallengeEvent event = ChallengeEvent.builder()
                    .eventType(ChallengeEvent.EventType.SIGN_UP_SUCCESS)
                    .challengeId(challengeId)
                    .userId(userIdStr)
                    .spotId(challenge.getSpotId())
                    .triggerTime(LocalDateTime.now())
                    .build();
            rabbitMqHelper.publishChallengeEvent(event);
            return Result.success(orderId);
        } finally {
            // 无论成功失败都执行 Lua 解锁，保证锁可释放。
            unlock(lockKey, lockOwner);
        }
    }

    @Override
    @Transactional
    public Result<Void> cancelChallenge(Long challengeId) {
        if (challengeId == null) {
            return Result.error("挑战ID不能为空");
        }
        String userIdStr = UserHolder.getUser().getId();
        long userId;
        try {
            userId = Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            return Result.error("用户ID格式错误！");
        }

        // 1) 找到“当前用户可取消”的报名记录（排除已取消状态）。
        ChallengeParticipation participation = lambdaQuery()
                .eq(ChallengeParticipation::getUserId, userId)
                .eq(ChallengeParticipation::getChallengeId, challengeId)
                .ne(ChallengeParticipation::getStatus, 3)
                .one();
        if (participation == null) {
            return Result.error("未找到可取消的报名记录");
        }

        // 2) 回收名额（仅在 joinedSlots > 0 时递减）。
        boolean decSuccess = challengeService.update()
                .setSql("joinedSlots = joinedSlots - 1")
                .eq("id", challengeId)
                .gt("joinedSlots", 0)
                .update();
        if (!decSuccess) {
            return Result.error("取消失败，请稍后重试");
        }
        // 取消报名后失效挑战详情缓存，避免名额字段脏读。
        evictChallengeCache(challengeId);

        participation.setStatus(3);
        participation.setResult("已取消");
        boolean updated = updateById(participation);
        if (!updated) {
            throw new IllegalStateException("取消报名状态更新失败");
        }

        Challenge challenge = challengeService.getById(challengeId);
        // 3) 发布取消事件，异步通知和榜单修正由 MQ 消费者处理。
        ChallengeEvent event = ChallengeEvent.builder()
                .eventType(ChallengeEvent.EventType.CANCEL_SUCCESS)
                .challengeId(challengeId)
                .userId(userIdStr)
                .spotId(challenge == null ? null : challenge.getSpotId())
                .triggerTime(LocalDateTime.now())
                .build();
        rabbitMqHelper.publishChallengeEvent(event);
        return Result.success();
    }

    @Override
    public Result<PageResult<ChallengeParticipation>> listMyChallenges(String userId, Integer page, Integer size) {
        if (userId == null || userId.isBlank()) {
            return Result.error("用户未登录");
        }
        long userIdLong;
        try {
            userIdLong = Long.parseLong(userId);
        } catch (NumberFormatException e) {
            return Result.error("用户ID格式错误");
        }

        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : Math.min(size, 50);

        Page<ChallengeParticipation> mpPage = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<ChallengeParticipation> qw = new LambdaQueryWrapper<>();
        qw.eq(ChallengeParticipation::getUserId, userIdLong)
                .orderByDesc(ChallengeParticipation::getCreateTime);

        Page<ChallengeParticipation> resultPage = page(mpPage, qw);

        PageResult<ChallengeParticipation> result = new PageResult<>();
        result.setTotal(resultPage.getTotal());
        result.setPage(pageNo);
        result.setSize(pageSize);
        result.setRecords(resultPage.getRecords());
        return Result.success(result);
    }

    private Long parseUserId(String userId) {
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private ChallengeParticipation buildParticipation(Long challengeId, Long spotId, Long userId) {
        ChallengeParticipation challengeParticipation = new ChallengeParticipation();
        challengeParticipation.setUserId(userId);
        challengeParticipation.setChallengeId(challengeId);
        challengeParticipation.setSpotId(spotId);
        return challengeParticipation;
    }

    private String buildJoinLockKey(Long challengeId, Long userId) {
        // 锁 key 设计：lock:challenge:join:{challengeId}:{userId}
        return LOCK_CHALLENGE_JOIN_KEY + challengeId + ":" + userId;
    }

    private boolean tryJoinLock(String key, String owner) {
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(key, owner, 10, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(locked);
    }

    private void unlock(String key, String owner) {
        // KEYS[1]=lockKey, ARGV[1]=owner，脚本内做“比对后删除”原子操作。
        stringRedisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), owner);
    }

    private String buildLockOwner() {
        return LOCK_VALUE_PREFIX + ":" + Thread.currentThread().threadId();
    }

    private void evictChallengeCache(Long challengeId) {
        if (challengeId == null) {
            return;
        }
        stringRedisTemplate.delete(CACHE_CHALLENGE_KEY + challengeId);
    }
}
