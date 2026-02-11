package com.example.sportx.Service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.sportx.Entity.Challenge;
import com.example.sportx.Entity.ChallengeEvent;
import com.example.sportx.Entity.dto.ChallengeListQueryDto;
import com.example.sportx.Mapper.ChallengeMapper;
import com.example.sportx.Service.ChallengeService;
import com.example.sportx.Utils.RabbitMqHelper;
import com.example.sportx.Entity.vo.PageResult;
import com.example.sportx.Entity.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChallengeServiceImpl extends ServiceImpl<ChallengeMapper, Challenge> implements ChallengeService {

    private final RabbitMqHelper rabbitMqHelper;

    @Transactional
    @Override
    public void addChallenge(Challenge challenge) {
        save(challenge);
        scheduleReminders(challenge);
    }

    @Override
    public Result<PageResult<Challenge>> listChallenges(ChallengeListQueryDto queryDto) {
        int pageNo = queryDto.getPage() == null || queryDto.getPage() < 1 ? 1 : queryDto.getPage();
        int pageSize = queryDto.getSize() == null || queryDto.getSize() < 1 ? 10 : Math.min(queryDto.getSize(), 50);

        Page<Challenge> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<Challenge> qw = new LambdaQueryWrapper<>();

        if (queryDto.getSpotId() != null) {
            qw.eq(Challenge::getSpotId, queryDto.getSpotId());
        }
        if (queryDto.getKeyword() != null && !queryDto.getKeyword().isBlank()) {
            qw.like(Challenge::getChallengeName, queryDto.getKeyword().trim());
        }

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
        Challenge challenge = getById(challengeId);
        if (challenge == null) {
            return Result.error("挑战不存在");
        }
        return Result.success(challenge);
    }

    private void scheduleReminders(Challenge challenge) {
        if (challenge == null || challenge.getId() == null) {
            return;
        }
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
