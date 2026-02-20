package com.example.sportx.Service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.sportx.Entity.ChallengeEvent;
import com.example.sportx.Entity.FailedMessage;
import com.example.sportx.Entity.vo.PageResult;
import com.example.sportx.Entity.vo.Result;
import com.example.sportx.Mapper.FailedMessageMapper;
import com.example.sportx.Service.FailedMessageService;
import com.example.sportx.Utils.RabbitMqHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class FailedMessageServiceImpl implements FailedMessageService {

    private static final String STATUS_NEW = "NEW";
    private static final String STATUS_RETRIED = "RETRIED";
    private static final String STATUS_RETRY_FAILED = "RETRY_FAILED";

    private final FailedMessageMapper failedMessageMapper;
    private final RabbitMqHelper rabbitMqHelper;
    private final ObjectMapper objectMapper;

    @Override
    public void recordDeadLetter(Message message, String reason) {
        // 将死信消息完整落库，后续可按状态筛选、人工回放。
        if (message == null) {
            return;
        }
        FailedMessage failed = new FailedMessage();
        failed.setQueueName(message.getMessageProperties() == null ? null : message.getMessageProperties().getConsumerQueue());
        failed.setExchangeName(message.getMessageProperties() == null ? null : message.getMessageProperties().getReceivedExchange());
        failed.setRoutingKey(message.getMessageProperties() == null ? null : message.getMessageProperties().getReceivedRoutingKey());
        failed.setPayload(new String(message.getBody(), StandardCharsets.UTF_8));
        failed.setReason(reason);
        failed.setStatus(STATUS_NEW);
        failed.setRetryCount(0);
        failedMessageMapper.insert(failed);
    }

    @Override
    public Result<PageResult<FailedMessage>> listFailedMessages(Integer page, Integer size, String status) {
        // 运维排查入口：支持分页和状态过滤。
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : Math.min(size, 50);

        Page<FailedMessage> mpPage = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<FailedMessage> qw = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            qw.eq(FailedMessage::getStatus, status.trim().toUpperCase());
        }
        qw.orderByDesc(FailedMessage::getCreateTime);

        Page<FailedMessage> resultPage = failedMessageMapper.selectPage(mpPage, qw);
        PageResult<FailedMessage> result = new PageResult<>();
        result.setTotal(resultPage.getTotal());
        result.setPage(pageNo);
        result.setSize(pageSize);
        result.setRecords(resultPage.getRecords());
        return Result.success(result);
    }

    @Override
    public Result<Void> retryFailedMessage(Long id) {
        // 手动回放入口：把失败 payload 重新投递到挑战事件交换机。
        if (id == null) {
            return Result.error("失败消息ID不能为空");
        }
        FailedMessage failedMessage = failedMessageMapper.selectById(id);
        if (failedMessage == null) {
            return Result.error("失败消息不存在");
        }

        int nextRetry = (failedMessage.getRetryCount() == null ? 0 : failedMessage.getRetryCount()) + 1;
        try {
            // payload 按 ChallengeEvent 反序列化，复用同一事件链路。
            ChallengeEvent event = objectMapper.readValue(failedMessage.getPayload(), ChallengeEvent.class);
            rabbitMqHelper.publishChallengeEvent(event);

            // 回放成功后更新状态与重试次数。
            FailedMessage toUpdate = new FailedMessage();
            toUpdate.setId(id);
            toUpdate.setRetryCount(nextRetry);
            toUpdate.setStatus(STATUS_RETRIED);
            failedMessageMapper.updateById(toUpdate);
            return Result.success();
        } catch (Exception exception) {
            log.error("Retry failed message error, id={}", id, exception);
            // 回放失败保留错误原因，便于再次排查。
            FailedMessage toUpdate = new FailedMessage();
            toUpdate.setId(id);
            toUpdate.setRetryCount(nextRetry);
            toUpdate.setStatus(STATUS_RETRY_FAILED);
            toUpdate.setReason("manual retry failed: " + exception.getMessage());
            failedMessageMapper.updateById(toUpdate);
            return Result.error("重试失败：" + exception.getMessage());
        }
    }
}
