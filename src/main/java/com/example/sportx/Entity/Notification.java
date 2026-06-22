package com.example.sportx.Entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notifications")   // 表名为复数；否则 MyBatis-Plus 默认推导成单数 notification 导致表不存在
public class Notification {
    private Long id;
    private String userId;
    private String type;
    private String title;
    private String content;
    private Boolean isRead;
    private LocalDateTime createTime;
}
