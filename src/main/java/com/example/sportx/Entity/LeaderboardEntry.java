package com.example.sportx.Entity;

import lombok.Data;

/**
 * 榜单快照明细，关联到具体榜单主体。
 */
@Data
public class LeaderboardEntry {

    private Long id;
    private Long snapshotId; // 对应 LeaderboardSnapshot 主键
    private Integer rank;    // 排名
    private String targetId; // 榜单对象（如场馆ID）
    private Double score;    // 对应分值
    private String payload;  // 扩展字段（JSON），可存储名称、类型等
}

