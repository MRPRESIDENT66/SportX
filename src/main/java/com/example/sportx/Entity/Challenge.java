package com.example.sportx.Entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Challenge {
    private Long id;                  // 主键 ID

    private String challengeName;     // 挑战名称，例如 "巴黎月跑100KM"
    private String description;       // 挑战简介
    private Integer totalSlots;       // 总名额，例如只允许100人参与
    private Integer joinedSlots;      // 已参与人数

    private LocalDate startTime;      // 挑战开始日期
    private LocalDate endTime;        // 结束日期

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
