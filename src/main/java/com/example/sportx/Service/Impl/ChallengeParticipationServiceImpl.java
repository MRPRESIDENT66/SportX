package com.example.sportx.Service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.sportx.Entity.Challenge;
import com.example.sportx.Entity.ChallengeEvent;
import com.example.sportx.Entity.ChallengeParticipation;
import com.example.sportx.Entity.OutboxEvent;
import com.example.sportx.Entity.User;
import com.example.sportx.Entity.vo.PageResult;
import com.example.sportx.Entity.vo.Result;
import com.example.sportx.Mapper.ChallengeParMapper;
import com.example.sportx.Mapper.OutboxEventMapper;
import com.example.sportx.Service.ChallengeParticipationService;
import com.example.sportx.Service.ChallengeService;
import com.example.sportx.Utils.RedisIdGenerator;
import com.example.sportx.Utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static com.example.sportx.Utils.RedisConstants.LOCK_CHALLENGE_JOIN_KEY;

@Service
@RequiredArgsConstructor
public class ChallengeParticipationServiceImpl extends ServiceImpl<ChallengeParMapper, ChallengeParticipation> implements ChallengeParticipationService {

    private final ChallengeService challengeService;
    private final RedisIdGenerator redisIdGenerator;
    private final OutboxEventMapper outboxEventMapper;
    private final RedissonClient redissonClient;

    @Override
    @Transactional
    public Result<Long> joinChallenge(Long challengeId) {
        // 1) 基础业务校验：活动存在、时间窗口、名额预检。
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

        // 3) Redisson 分布式锁（粒度=用户+挑战）：仅作接口防抖/防重复提交，拦掉用户连点。
        //    正确性不依赖它——超卖由原子条件 UPDATE 保证，重复报名由唯一索引保证。
        //    用 Redisson 替代手写 SETNX+Lua：可重入、解锁原子性与 owner 校验由其内部 Lua 保证，
        //    无需自己维护脚本/owner，更健壮。waitTime=0 抢不到立即拒绝（防抖语义，不阻塞）；
        //    leaseTime=10s 兜底释放，防止持锁线程崩溃导致死锁。
        String lockKey = buildJoinLockKey(challengeId, userId);
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked;
        try {
            locked = lock.tryLock(0, 10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.error("请求被中断，请稍后重试");
        }
        if (!locked) {
            return Result.error("请求过于频繁，请稍后重试");
        }
        try {
            // 4) 快速幂等预检：已报名直接返回，避免走到 INSERT 才报唯一键异常。
            long count = lambdaQuery()
                    .eq(ChallengeParticipation::getUserId, userId)
                    .eq(ChallengeParticipation::getChallengeId, challengeId)
                    .count();
            if (count > 0) {
                return Result.error("你已报名该挑战，请勿重复报名！");
            }

            // 5) 原子条件 UPDATE 防超卖：WHERE joined_slots < total_slots 在行锁下原子执行，
            //    无论多少并发同时到达，只要有空位就成功，精确满员才失败，不误杀合法请求。
            //    注意：update() 的字符串列名不会自动驼峰转下划线，必须用真实列名 joined_slots。
            boolean success = challengeService.update()
                    .setSql("joined_slots = joined_slots + 1")
                    .eq("id", challengeId)
                    .lt("joined_slots", challenge.getTotalSlots())
                    .update();
            if (!success) {
                return Result.error("报名失败，名额已满！");
            }

            // 6) 写报名记录，唯一索引兜底防止极端并发下的重复落库。
            ChallengeParticipation participation = buildParticipation(challengeId, challenge.getSpotId(), userId);
            long participationId = redisIdGenerator.nextId("participation");
            participation.setId(participationId);
            try {
                save(participation);
            } catch (DuplicateKeyException e) {
                throw new IllegalArgumentException("你已报名该挑战，请勿重复操作");
            }

            // 7) 同事务写 outbox 记录：与上方所有 DB 写操作原子提交。
            //    relay 在事务提交后异步读取并投递到 MQ，彻底隔离"业务写"与"消息投递"。
            //    即使应用在 COMMIT 后立刻宕机，relay 重启后仍能从 outbox 表恢复投递，不丢事件。
            ChallengeEvent event = ChallengeEvent.builder()
                    .eventType(ChallengeEvent.EventType.SIGN_UP_SUCCESS)
                    .challengeId(challengeId)
                    .userId(userIdStr)
                    .spotId(challenge.getSpotId())
                    .triggerTime(LocalDateTime.now())
                    .build();
            outboxEventMapper.insert(OutboxEvent.of(redisIdGenerator.nextId("outbox"), event));

            return Result.success(participationId);
        } finally {
            // 仅当锁仍由当前线程持有时才释放，避免租期已过被他人持有时误删（Redisson 内部 owner 校验）。
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
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

        // 1) 找到该用户对应的报名记录。
        ChallengeParticipation participation = lambdaQuery()
                .eq(ChallengeParticipation::getUserId, userId)
                .eq(ChallengeParticipation::getChallengeId, challengeId)
                .one();
        if (participation == null) {
            return Result.error("未找到报名记录");
        }

        // 2) CAS：仅当 status=1 时才更新为 3，防并发重复取消。
        boolean cancelled = lambdaUpdate()
                .eq(ChallengeParticipation::getId, participation.getId())
                .eq(ChallengeParticipation::getStatus, 1)
                .set(ChallengeParticipation::getStatus, 3)
                .set(ChallengeParticipation::getResult, "已取消")
                .update();
        if (!cancelled) {
            return Result.error("未找到可取消的报名记录");
        }

        // 3) status 抢占成功后原子回收名额（守卫 joined_slots > 0 防止下穿）。
        boolean decSuccess = challengeService.update()
                .setSql("joined_slots = joined_slots - 1")
                .eq("id", challengeId)
                .gt("joined_slots", 0)
                .update();
        if (!decSuccess) {
            // joinedSlots 已是 0（极端情况），抛异常让 @Transactional 自动回滚 status 变更，
            // 比手动补偿 UPDATE 更安全——手动补偿若也失败会留下不一致状态。
            throw new IllegalStateException("取消失败：名额计数异常，已自动回滚");
        }

        // 4) 同事务写 outbox 记录，relay 提交后异步投递取消事件。
        Challenge challenge = challengeService.getById(challengeId);
        ChallengeEvent event = ChallengeEvent.builder()
                .eventType(ChallengeEvent.EventType.CANCEL_SUCCESS)
                .challengeId(challengeId)
                .userId(userIdStr)
                .spotId(challenge == null ? null : challenge.getSpotId())
                .triggerTime(LocalDateTime.now())
                .build();
        outboxEventMapper.insert(OutboxEvent.of(redisIdGenerator.nextId("outbox"), event));

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

    // ── private helpers ──────────────────────────────────────────────────────

    private Long parseUserId(String userId) {
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private ChallengeParticipation buildParticipation(Long challengeId, Long spotId, Long userId) {
        ChallengeParticipation p = new ChallengeParticipation();
        p.setUserId(userId);
        p.setChallengeId(challengeId);
        p.setSpotId(spotId);
        return p;
    }

    private String buildJoinLockKey(Long challengeId, Long userId) {
        return LOCK_CHALLENGE_JOIN_KEY + challengeId + ":" + userId;
    }
}
