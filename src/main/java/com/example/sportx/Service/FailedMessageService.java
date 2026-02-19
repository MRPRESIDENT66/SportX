package com.example.sportx.Service;

import com.example.sportx.Entity.FailedMessage;
import com.example.sportx.Entity.vo.PageResult;
import com.example.sportx.Entity.vo.Result;
import org.springframework.amqp.core.Message;

public interface FailedMessageService {
    void recordDeadLetter(Message message, String reason);

    Result<PageResult<FailedMessage>> listFailedMessages(Integer page, Integer size, String status);

    Result<Void> retryFailedMessage(Long id);
}
