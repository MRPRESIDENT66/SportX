package com.example.sportx.Service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.sportx.Entity.Challenge;
import com.example.sportx.Entity.ChallengeEvent;
import com.example.sportx.Entity.ChallengeParticipation;
import com.example.sportx.Entity.Result;
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
    public Result joinChallenge(Long id) {
        //1.查询活动
        Challenge challenge = challengeService.getById(id);
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
        //6.一人一单
        String userIdStr = UserHolder.getUser().getId();
        long count = query().eq("userid", userIdStr).eq("chanlengeId",id).count();
        if(count>0){
            return Result.error("该用户已经下单！");
        }
        //5.扣减余量
        boolean success= challengeService.update()
                .setSql("joinedSlots = joinedSlots +1")
                .eq("id", id).eq("joinedSlots",challenge.getJoinedSlots())
                .update();
        if(!success){
            return Result.error("活动名额不足！");
        }
        //6.创建订单
        ChallengeParticipation challengeParticipation = new ChallengeParticipation();
        long orderId = redisIDWorker.nextID("order");
        challengeParticipation.setId(orderId);
        String id1 = UserHolder.getUser().getId();
        long userId = Long.parseLong(id1);
        challengeParticipation.setUserId(userId);
        challengeParticipation.setChallengeId(id);
        challengeParticipation.setSpotId(challenge.getSpotId()); // 对齐场馆信息
        save(challengeParticipation);

        ChallengeEvent event = ChallengeEvent.builder()
                .eventType(ChallengeEvent.EventType.SIGN_UP_SUCCESS)
                .challengeId(id)
                .userId(UserHolder.getUser().getId())
                .spotId(challenge.getSpotId()) // 事件携带场馆 ID
                .triggerTime(LocalDateTime.now())
                .build();
        rabbitMqHelper.publishChallengeEvent(event);
        //返回订单id
        return Result.success(orderId);
    }
}
