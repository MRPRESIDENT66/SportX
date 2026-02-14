package com.example.sportx.Entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Notification {
    private Long id;
    private String userId;
    private String type;
    private String title;
    private String content;
    private Boolean isRead;
    private LocalDateTime createTime;
}
