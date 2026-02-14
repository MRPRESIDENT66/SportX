package com.example.sportx.Service.Impl;

import com.example.sportx.Entity.ChallengeEvent;
import com.example.sportx.Entity.ChallengeParticipation;
import com.example.sportx.Entity.Notification;
import com.example.sportx.Mapper.ChallengeParMapper;
import com.example.sportx.Mapper.NotificationMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private ChallengeParMapper challengeParMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void notifySignupSuccess_shouldInsertNotificationForCurrentUser() {
        ChallengeEvent event = ChallengeEvent.builder()
                .eventType(ChallengeEvent.EventType.SIGN_UP_SUCCESS)
                .challengeId(100L)
                .userId("123")
                .triggerTime(LocalDateTime.now())
                .build();

        notificationService.notifySignupSuccess(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(captor.capture());
        Notification inserted = captor.getValue();
        assertThat(inserted.getUserId()).isEqualTo("123");
        assertThat(inserted.getType()).isEqualTo("SIGN_UP_SUCCESS");
        assertThat(inserted.getTitle()).isEqualTo("报名成功");
        assertThat(inserted.getIsRead()).isFalse();
    }

    @Test
    void notifyCancelSuccess_shouldInsertNotificationForCurrentUser() {
        ChallengeEvent event = ChallengeEvent.builder()
                .eventType(ChallengeEvent.EventType.CANCEL_SUCCESS)
                .challengeId(100L)
                .userId("123")
                .triggerTime(LocalDateTime.now())
                .build();

        notificationService.notifyCancelSuccess(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(captor.capture());
        Notification inserted = captor.getValue();
        assertThat(inserted.getUserId()).isEqualTo("123");
        assertThat(inserted.getType()).isEqualTo("CANCEL_SUCCESS");
        assertThat(inserted.getTitle()).isEqualTo("取消报名成功");
        assertThat(inserted.getIsRead()).isFalse();
    }

    @Test
    void notifyStartReminder_shouldFanOutToAllParticipants() {
        ChallengeParticipation p1 = new ChallengeParticipation();
        p1.setUserId(1L);
        ChallengeParticipation p2 = new ChallengeParticipation();
        p2.setUserId(2L);
        when(challengeParMapper.selectList(any())).thenReturn(List.of(p1, p2));

        ChallengeEvent event = ChallengeEvent.builder()
                .eventType(ChallengeEvent.EventType.START_REMINDER)
                .challengeId(100L)
                .triggerTime(LocalDateTime.now())
                .build();

        notificationService.notifyStartReminder(event);

        verify(notificationMapper, times(2)).insert(any(Notification.class));
    }

    @Test
    void notifyEndReminder_shouldSkipInvalidUserAndInsertForValidUsers() {
        ChallengeParticipation p1 = new ChallengeParticipation();
        p1.setUserId(1L);
        ChallengeParticipation p2 = new ChallengeParticipation();
        p2.setUserId(null);
        when(challengeParMapper.selectList(any())).thenReturn(List.of(p1, p2));

        ChallengeEvent event = ChallengeEvent.builder()
                .eventType(ChallengeEvent.EventType.END_REMINDER)
                .challengeId(100L)
                .triggerTime(LocalDateTime.now())
                .build();

        notificationService.notifyEndReminder(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(captor.capture());
        Notification inserted = captor.getValue();
        assertThat(inserted.getUserId()).isEqualTo("1");
        assertThat(inserted.getType()).isEqualTo("END_REMINDER");
        assertThat(inserted.getTitle()).isEqualTo("结束提醒");
    }
}
