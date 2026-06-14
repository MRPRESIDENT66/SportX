CREATE TABLE IF NOT EXISTS `leaderboard_event_log` (
  `id`           BIGINT       NOT NULL COMMENT '全局唯一ID',
  `user_id`      VARCHAR(32)  NOT NULL COMMENT '用户ID',
  `challenge_id` BIGINT       NOT NULL COMMENT '挑战ID',
  `spot_id`      BIGINT       DEFAULT NULL COMMENT '场馆ID',
  `event_type`   VARCHAR(32)  NOT NULL COMMENT 'SIGN_UP_SUCCESS | CANCEL_SUCCESS',
  `user_delta`   DOUBLE       NOT NULL COMMENT '本次对用户积分的变更量',
  `spot_delta`   DOUBLE       NOT NULL COMMENT '本次对场馆热度的变更量',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  -- 唯一约束：同一用户对同一挑战的同类事件只能计一次
  -- 是幂等兜底线，重复消费时 INSERT 抛 DuplicateKeyException，ZINCRBY 不会执行
  UNIQUE KEY `uk_leaderboard_event` (`user_id`, `challenge_id`, `event_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排行榜事件流水：幂等兜底 + 对账基准';
