package com.example.sportx.Entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 榜单快照主表，用于记录一次快照生成任务。
 */
@Data
public class LeaderboardSnapshot {

    private Long id;
    private String type;               // 榜单类型，例如 spot_heat
    private LocalDateTime periodStart; // 统计周期开始
    private LocalDateTime periodEnd;   // 统计周期结束
    private LocalDateTime createdAt;   // 快照生成时间
}

