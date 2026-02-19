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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.example.sportx.Utils.RedisConstants.LOCK_CHALLENGE_JOIN_KEY;

@Service
@RequiredArgsConstructor
public class ChallengeParticipationServiceImpl extends ServiceImpl<ChallengeParMapper, ChallengeParticipation> implements ChallengeParticipationService {
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
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

        User currentUser = UserHolder.getUser();
        if (currentUser == null || currentUser.getId() == null) {
            return Result.error("用户未登录！");
        }
        String userIdStr = currentUser.getId();
        Long userId = parseUserId(userIdStr);
        if (userId == null) {
            return Result.error("用户ID格式错误！");
        }

        String lockKey = buildJoinLockKey(challengeId, userId);
        String lockOwner = buildLockOwner();
        boolean locked = tryJoinLock(lockKey, lockOwner);
        if (!locked) {
            return Result.error("请求过于频繁，请稍后重试");
        }
        try {
            long count = lambdaQuery()
                    .eq(ChallengeParticipation::getUserId, userId)
                    .eq(ChallengeParticipation::getChallengeId, challengeId)
                    .count();
            if (count > 0) {
                return Result.error("该用户已经下单！");
            }

            boolean success = challengeService.update()
                    .setSql("joinedSlots = joinedSlots +1")
                    .eq("id", challengeId)
                    .eq("joinedSlots", challenge.getJoinedSlots())
                    .update();
            if (!success) {
                return Result.error("活动名额不足！");
            }

            ChallengeParticipation challengeParticipation = buildParticipation(challengeId, challenge.getSpotId(), userId);
            long orderId = redisIDWorker.nextID("order");
            challengeParticipation.setId(orderId);
            save(challengeParticipation);

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

        ChallengeParticipation participation = lambdaQuery()
                .eq(ChallengeParticipation::getUserId, userId)
                .eq(ChallengeParticipation::getChallengeId, challengeId)
                .ne(ChallengeParticipation::getStatus, 3)
                .one();
        if (participation == null) {
            return Result.error("未找到可取消的报名记录");
        }

        boolean decSuccess = challengeService.update()
                .setSql("joinedSlots = joinedSlots - 1")
                .eq("id", challengeId)
                .gt("joinedSlots", 0)
                .update();
        if (!decSuccess) {
            return Result.error("取消失败，请稍后重试");
        }

        participation.setStatus(3);
        participation.setResult("已取消");
        boolean updated = updateById(participation);
        if (!updated) {
            throw new IllegalStateException("取消报名状态更新失败");
        }

        Challenge challenge = challengeService.getById(challengeId);
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
        return LOCK_CHALLENGE_JOIN_KEY + challengeId + ":" + userId;
    }

    private boolean tryJoinLock(String key, String owner) {
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(key, owner, 10, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(locked);
    }

    private void unlock(String key, String owner) {
        stringRedisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), owner);
    }

    private String buildLockOwner() {
        return LOCK_VALUE_PREFIX + ":" + Thread.currentThread().threadId();
    }
}
