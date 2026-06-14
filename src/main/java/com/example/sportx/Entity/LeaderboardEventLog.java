package com.example.sportx.Entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 排行榜事件流水，双重职责：
 * 1. 幂等兜底：唯一约束 (user_id, challenge_id, event_type) 保证 ZINCRBY 不重复执行。
 * 2. 对账基准：SUM(user_delta) / SUM(spot_delta) 可与 Redis ZSet score 比对，发现漂移立刻修正。
 */
@Data
@TableName("leaderboard_event_log")
public class LeaderboardEventLog {

    private Long id;
    private String userId;
    private Long challengeId;
    private Long spotId;
    private String eventType;
    private Double userDelta;
    private Double spotDelta;
    private LocalDateTime createdAt;
}
