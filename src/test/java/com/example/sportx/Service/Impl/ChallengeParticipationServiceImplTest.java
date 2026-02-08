package com.example.sportx.Service.Impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.UpdateChainWrapper;
import com.example.sportx.Entity.Challenge;
import com.example.sportx.Entity.ChallengeEvent;
import com.example.sportx.Entity.ChallengeParticipation;
import com.example.sportx.Entity.vo.Result;
import com.example.sportx.Entity.User;
import com.example.sportx.Service.ChallengeService;
import com.example.sportx.Utils.RabbitMqHelper;
import com.example.sportx.Utils.RedisIDWorker;
import com.example.sportx.Utils.UserHolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChallengeParticipationServiceImplTest {

    @Mock
    private ChallengeService challengeService;

    @Mock
    private RedisIDWorker redisIDWorker;

    @Mock
    private RabbitMqHelper rabbitMqHelper;

    @Test
    void joinChallenge_shouldReturnErrorWhenChallengeNotFound() {
        ChallengeParticipationServiceImpl challengeParticipationService =
                new ChallengeParticipationServiceImpl(challengeService, redisIDWorker, rabbitMqHelper);
        when(challengeService.getById(1L)).thenReturn(null);

        Result result = challengeParticipationService.joinChallenge(1L);

        assertThat(result.getCode()).isEqualTo(1);
        assertThat(result.getMessage()).isEqualTo("活动不存在！");
        verify(rabbitMqHelper, never()).publishChallengeEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void joinChallenge_shouldReturnErrorWhenRegistrationNotStarted() {
        ChallengeParticipationServiceImpl challengeParticipationService =
                new ChallengeParticipationServiceImpl(challengeService, redisIDWorker, rabbitMqHelper);
        Challenge challenge = baseChallenge();
        challenge.setStartTime(LocalDate.now().plusDays(1));
        when(challengeService.getById(1L)).thenReturn(challenge);

        Result result = challengeParticipationService.joinChallenge(1L);

        assertThat(result.getCode()).isEqualTo(1);
        assertThat(result.getMessage()).isEqualTo("活动报名还未开始！");
    }

    @Test
    void joinChallenge_shouldReturnErrorWhenRegistrationEnded() {
        ChallengeParticipationServiceImpl challengeParticipationService =
                new ChallengeParticipationServiceImpl(challengeService, redisIDWorker, rabbitMqHelper);
        Challenge challenge = baseChallenge();
        challenge.setEndTime(LocalDate.now().minusDays(1));
        when(challengeService.getById(1L)).thenReturn(challenge);

        Result result = challengeParticipationService.joinChallenge(1L);

        assertThat(result.getCode()).isEqualTo(1);
        assertThat(result.getMessage()).isEqualTo("活动报名已经结束！");
    }

    @Test
    void joinChallenge_shouldReturnErrorWhenSlotsAreFull() {
        ChallengeParticipationServiceImpl challengeParticipationService =
                new ChallengeParticipationServiceImpl(challengeService, redisIDWorker, rabbitMqHelper);
        Challenge challenge = baseChallenge();
        challenge.setTotalSlots(100);
        challenge.setJoinedSlots(100);
        when(challengeService.getById(1L)).thenReturn(challenge);

        Result result = challengeParticipationService.joinChallenge(1L);

        assertThat(result.getCode()).isEqualTo(1);
        assertThat(result.getMessage()).isEqualTo("活动名额不足！");
    }

    @Test
    void joinChallenge_shouldReturnErrorWhenUserAlreadyJoined() {
        ChallengeParticipationServiceImpl service = spy(
                new ChallengeParticipationServiceImpl(challengeService, redisIDWorker, rabbitMqHelper)
        );
        LambdaQueryChainWrapper<ChallengeParticipation> queryChain =
                org.mockito.Mockito.mock(LambdaQueryChainWrapper.class);

        User user = new User();
        user.setId("123");
        UserHolder.saveUser(user);
        try {
            when(challengeService.getById(1L)).thenReturn(baseChallenge());
            doReturn(queryChain).when(service).lambdaQuery();
            when(queryChain.eq(any(), any())).thenReturn(queryChain);
            when(queryChain.count()).thenReturn(1L);

            Result result = service.joinChallenge(1L);

            assertThat(result.getCode()).isEqualTo(1);
            assertThat(result.getMessage()).isEqualTo("该用户已经下单！");
            verify(challengeService, never()).update();
            verify(rabbitMqHelper, never()).publishChallengeEvent(any());
        } finally {
            UserHolder.removeUser();
        }
    }

    @Test
    void joinChallenge_shouldCreateOrderAndPublishEventWhenValid() {
        ChallengeParticipationServiceImpl service = spy(
                new ChallengeParticipationServiceImpl(challengeService, redisIDWorker, rabbitMqHelper)
        );
        LambdaQueryChainWrapper<ChallengeParticipation> queryChain =
                org.mockito.Mockito.mock(LambdaQueryChainWrapper.class);
        UpdateChainWrapper<Challenge> updateChain = org.mockito.Mockito.mock(UpdateChainWrapper.class);

        User user = new User();
        user.setId("123");
        UserHolder.saveUser(user);
        try {
            when(challengeService.getById(1L)).thenReturn(baseChallenge());
            doReturn(queryChain).when(service).lambdaQuery();
            when(queryChain.eq(any(), any())).thenReturn(queryChain);
            when(queryChain.count()).thenReturn(0L);
            when(challengeService.update()).thenReturn(updateChain);
            when(updateChain.setSql(any())).thenReturn(updateChain);
            when(updateChain.eq(any(), any())).thenReturn(updateChain);
            when(updateChain.update()).thenReturn(true);
            when(redisIDWorker.nextID("order")).thenReturn(99L);
            doReturn(true).when(service).save(any(ChallengeParticipation.class));

            Result result = service.joinChallenge(1L);

            assertThat(result.getCode()).isEqualTo(0);
            assertThat(result.getData()).isEqualTo(99L);

            ArgumentCaptor<ChallengeParticipation> participationCaptor =
                    ArgumentCaptor.forClass(ChallengeParticipation.class);
            verify(service).save(participationCaptor.capture());
            ChallengeParticipation saved = participationCaptor.getValue();
            assertThat(saved.getId()).isEqualTo(99L);
            assertThat(saved.getChallengeId()).isEqualTo(1L);
            assertThat(saved.getSpotId()).isEqualTo(10L);
            assertThat(saved.getUserId()).isEqualTo(123L);

            ArgumentCaptor<ChallengeEvent> eventCaptor = ArgumentCaptor.forClass(ChallengeEvent.class);
            verify(rabbitMqHelper).publishChallengeEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().getEventType()).isEqualTo(ChallengeEvent.EventType.SIGN_UP_SUCCESS);
        } finally {
            UserHolder.removeUser();
        }
    }

    private Challenge baseChallenge() {
        Challenge challenge = new Challenge();
        challenge.setId(1L);
        challenge.setSpotId(10L);
        challenge.setStartTime(LocalDate.now().minusDays(1));
        challenge.setEndTime(LocalDate.now().plusDays(1));
        challenge.setTotalSlots(100);
        challenge.setJoinedSlots(10);
        return challenge;
    }
}
