package com.example.sportx.Service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.sportx.Entity.Challenge;
import com.example.sportx.Entity.ChallengeEvent;
import com.example.sportx.Entity.ChallengeParticipation;
import com.example.sportx.Entity.vo.Result;
import com.example.sportx.Mapper.ChallengeParMapper;
import com.example.sportx.Service.ChallengeParticipationService;
import com.example.sportx.Service.ChallengeService;
import com.example.sportx.Utils.RabbitMqHelper;
import com.example.sportx.Utils.RedisIDWorker;
import com.example.sportx.Utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChallengeParticipationServiceImpl extends ServiceImpl<ChallengeParMapper, ChallengeParticipation> implements ChallengeParticipationService {

    private final ChallengeService challengeService;
    private final RedisIDWorker redisIDWorker;
    private final RabbitMqHelper rabbitMqHelper;

    @Override
    public Result<Long> joinChallenge(Long challengeId) {
        //1.查询活动
        Challenge challenge = challengeService.getById(challengeId);
        //2.判断活动是否可以报名
        if(challenge==null){
            return Result.error("活动不存在！");
        }
        if(challenge.getStartTime().isAfter(LocalDate.now())){
            return Result.error("活动报名还未开始！");
        }
        //3.判断活动是否不可以报名了
        if(challenge.getEndTime().isBefore(LocalDate.now())){
            return Result.error("活动报名已经结束！");
        }
        //4.活动余量是否充足
//        if(challenge.getTotalSlots()<1){
        if(challenge.getJoinedSlots() >= challenge.getTotalSlots()){
            return Result.error("活动名额不足！");
        }
        String userIdStr = UserHolder.getUser().getId();
        long userId;
        try {
            userId = Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            return Result.error("用户ID格式错误！");
        }
        //6.一人一单
        long count = lambdaQuery()
                .eq(ChallengeParticipation::getUserId, userId)
                .eq(ChallengeParticipation::getChallengeId, challengeId)
                .count();
        if(count>0){
            return Result.error("该用户已经下单！");
        }
        //5.扣减余量
        boolean success= challengeService.update()
                .setSql("joinedSlots = joinedSlots +1")
                .eq("id", challengeId).eq("joinedSlots",challenge.getJoinedSlots())
                .update();
        if(!success){
            return Result.error("活动名额不足！");
        }
        //6.创建订单
        ChallengeParticipation challengeParticipation = new ChallengeParticipation();
        long orderId = redisIDWorker.nextID("order");
        challengeParticipation.setId(orderId);
        challengeParticipation.setUserId(userId);
        challengeParticipation.setChallengeId(challengeId);
        challengeParticipation.setSpotId(challenge.getSpotId()); // 对齐场馆信息
        save(challengeParticipation);

        ChallengeEvent event = ChallengeEvent.builder()
                .eventType(ChallengeEvent.EventType.SIGN_UP_SUCCESS)
                .challengeId(challengeId)
                .userId(userIdStr)
                .spotId(challenge.getSpotId()) // 事件携带场馆 ID
                .triggerTime(LocalDateTime.now())
                .build();
        rabbitMqHelper.publishChallengeEvent(event);
        //返回订单id
        return Result.success(orderId);
    }
}
