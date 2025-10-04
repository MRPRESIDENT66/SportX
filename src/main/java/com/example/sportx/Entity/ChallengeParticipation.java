package com.example.sportx.Entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChallengeParticipation {
    private Long id;
    private Long userId;         // 用户ID
    private Long challengeId;    // 挑战ID

    private Integer status;      // 报名状态：0未开始，1进行中，2已完成，3取消等
    private Integer score;       // 成绩/得分（可选）

    private String result;       // 挑战结果说明（例如："已完成"、"失败"）

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
