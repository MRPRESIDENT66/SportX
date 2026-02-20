package com.example.sportx.Service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.sportx.Entity.Challenge;
import com.example.sportx.Entity.ChallengeEvent;
import com.example.sportx.Entity.dto.ChallengeListQueryDto;
import com.example.sportx.Mapper.ChallengeMapper;
import com.example.sportx.Service.ChallengeService;
import com.example.sportx.Utils.CacheClient;
import com.example.sportx.Utils.RabbitMqHelper;
import com.example.sportx.Entity.vo.PageResult;
import com.example.sportx.Entity.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static com.example.sportx.Utils.RedisConstants.CACHE_CHALLENGE_KEY;
import static com.example.sportx.Utils.RedisConstants.CACHE_CHALLENGE_TTL;

@Service
@RequiredArgsConstructor
public class ChallengeServiceImpl extends ServiceImpl<ChallengeMapper, Challenge> implements ChallengeService {

    private final RabbitMqHelper rabbitMqHelper;
    private final CacheClient cacheClient;

    @Transactional
    @Override
    public void addChallenge(Challenge challenge) {
        // 创建挑战后立即注册“开赛/结束”提醒事件，后续由调度器按触发时间投递。
        save(challenge);
        scheduleReminders(challenge);
    }

    @Override
    public Result<PageResult<Challenge>> listChallenges(ChallengeListQueryDto queryDto) {
        // 统一处理分页参数，防止调用方传入异常页码或过大 size。
        int pageNo = queryDto.getPage() == null || queryDto.getPage() < 1 ? 1 : queryDto.getPage();
        int pageSize = queryDto.getSize() == null || queryDto.getSize() < 1 ? 10 : Math.min(queryDto.getSize(), 50);

        Page<Challenge> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<Challenge> qw = new LambdaQueryWrapper<>();

        // 条件过滤：场馆、关键词。
        if (queryDto.getSpotId() != null) {
            qw.eq(Challenge::getSpotId, queryDto.getSpotId());
        }
        if (queryDto.getKeyword() != null && !queryDto.getKeyword().isBlank()) {
            qw.like(Challenge::getChallengeName, queryDto.getKeyword().trim());
        }

        // 状态过滤基于当前日期推导，避免单独维护状态字段导致不一致。
        LocalDate today = LocalDate.now();
        String status = queryDto.getStatus();
        if (status != null && !status.isBlank()) {
            switch (status.trim().toLowerCase()) {
                case "upcoming":
                    qw.gt(Challenge::getStartTime, today);
                    break;
                case "ongoing":
                    qw.le(Challenge::getStartTime, today).ge(Challenge::getEndTime, today);
                    break;
                case "ended":
                    qw.lt(Challenge::getEndTime, today);
                    break;
                default:
                    return Result.error("status参数非法，仅支持 upcoming/ongoing/ended");
            }
        }

        qw.orderByDesc(Challenge::getCreateTime);
        Page<Challenge> resultPage = page(page, qw);

        PageResult<Challenge> result = new PageResult<>();
        result.setTotal(resultPage.getTotal());
        result.setPage(pageNo);
        result.setSize(pageSize);
        result.setRecords(resultPage.getRecords());
        return Result.success(result);
    }

    @Override
    public Result<Challenge> getChallengeDetail(Long challengeId) {
        if (challengeId == null) {
            return Result.error("挑战ID不能为空");
        }
        // 挑战详情走缓存穿透保护，缓存未命中才回库。
        Challenge challenge = cacheClient.queryWithPassThrough(
                CACHE_CHALLENGE_KEY,
                challengeId,
                Challenge.class,
                this::getById,
                CACHE_CHALLENGE_TTL,
                TimeUnit.MINUTES
        );
        if (challenge == null) {
            return Result.error("挑战不存在");
        }
        return Result.success(challenge);
    }

    private void scheduleReminders(Challenge challenge) {
        if (challenge == null || challenge.getId() == null) {
            return;
        }
        // startTime/endTime 统一取当天 00:00 作为提醒触发点，示例项目先采用日粒度提醒。
        if (challenge.getStartTime() != null) {
            ChallengeEvent startEvent = ChallengeEvent.builder()
                    .eventType(ChallengeEvent.EventType.START_REMINDER)
                    .challengeId(challenge.getId())
                    .spotId(challenge.getSpotId())
                    .triggerTime(challenge.getStartTime().atStartOfDay())
                    .build();
            rabbitMqHelper.publishChallengeEvent(startEvent);
        }
        if (challenge.getEndTime() != null) {
            ChallengeEvent endEvent = ChallengeEvent.builder()
                    .eventType(ChallengeEvent.EventType.END_REMINDER)
                    .challengeId(challenge.getId())
                    .spotId(challenge.getSpotId())
                    .triggerTime(challenge.getEndTime().atStartOfDay())
                    .build();
            rabbitMqHelper.publishChallengeEvent(endEvent);
        }
    }
}
