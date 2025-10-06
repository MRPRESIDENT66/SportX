package com.example.sportx.RabbitMQ;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务：后续用于将场馆热度榜快照写入数据库。
 */
@Slf4j
@Component
public class LeaderboardSnapshotScheduler {

    @Scheduled(cron = "0 0 * * * *")
    public void snapshotSpotHeat() {
        // TODO: 读取 Redis 热度榜 Top-N，写入快照表
        log.debug("Spot heat leaderboard snapshot task triggered - TODO implement.");
    }
}

