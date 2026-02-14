package com.example.sportx.Service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.sportx.Entity.ChallengeEvent;
import com.example.sportx.Entity.ChallengeParticipation;
import com.example.sportx.Entity.Notification;
import com.example.sportx.Mapper.ChallengeParMapper;
import com.example.sportx.Mapper.NotificationMapper;
import com.example.sportx.Service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final ChallengeParMapper challengeParMapper;

    @Override
    public void notifySignupSuccess(ChallengeEvent event) {
        saveSingleUserNotification(
                event.getUserId(),
                event.getEventType().name(),
                "报名成功",
                String.format("你已成功报名挑战（ID=%s）", String.valueOf(event.getChallengeId()))
        );
        log.info("[Notify] signup success -> challenge={}, user={}, trigger={}",
                event.getChallengeId(),
                event.getUserId(),
                event.getTriggerTime());
    }

    @Override
    public void notifyCancelSuccess(ChallengeEvent event) {
        saveSingleUserNotification(
                event.getUserId(),
                event.getEventType().name(),
                "取消报名成功",
                String.format("你已取消挑战报名（ID=%s）", String.valueOf(event.getChallengeId()))
        );
        log.info("[Notify] cancel success -> challenge={}, user={}, trigger={}",
                event.getChallengeId(),
                event.getUserId(),
                event.getTriggerTime());
    }

    @Override
    public void notifyStartReminder(ChallengeEvent event) {
        notifyParticipants(
                event,
                "开赛提醒",
                String.format("你报名的挑战（ID=%s）即将开始", String.valueOf(event.getChallengeId()))
        );
        log.info("[Notify] start reminder -> challenge={}, trigger={}",
                event.getChallengeId(),
                event.getTriggerTime());
    }

    @Override
    public void notifyEndReminder(ChallengeEvent event) {
        notifyParticipants(
                event,
                "结束提醒",
                String.format("你报名的挑战（ID=%s）已结束", String.valueOf(event.getChallengeId()))
        );
        log.info("[Notify] end reminder -> challenge={}, trigger={}",
                event.getChallengeId(),
                event.getTriggerTime());
    }

    private void notifyParticipants(ChallengeEvent event, String title, String content) {
        if (event.getChallengeId() == null) {
            return;
        }
        LambdaQueryWrapper<ChallengeParticipation> qw = new LambdaQueryWrapper<>();
        qw.eq(ChallengeParticipation::getChallengeId, event.getChallengeId());
        List<ChallengeParticipation> participations = challengeParMapper.selectList(qw);
        if (participations == null || participations.isEmpty()) {
            return;
        }
        for (ChallengeParticipation participation : participations) {
            if (participation == null || participation.getUserId() == null) {
                continue;
            }
            saveSingleUserNotification(
                    String.valueOf(participation.getUserId()),
                    event.getEventType().name(),
                    title,
                    content
            );
        }
    }

    private void saveSingleUserNotification(String userId, String type, String title, String content) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setIsRead(false);
        notificationMapper.insert(notification);
    }
}
