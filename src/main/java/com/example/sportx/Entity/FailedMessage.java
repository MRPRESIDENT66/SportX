package com.example.sportx.Entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("failed_message")
public class FailedMessage {
    private Long id;
    private String queueName;
    private String exchangeName;
    private String routingKey;
    private String payload;
    private String reason;
    private String status;
    private Integer retryCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
