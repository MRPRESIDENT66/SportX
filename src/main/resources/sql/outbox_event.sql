CREATE TABLE IF NOT EXISTS `outbox_event` (
  `id`           BIGINT        NOT NULL COMMENT '全局唯一ID（RedisIdGenerator）',
  `event_type`   VARCHAR(64)   NOT NULL COMMENT '事件类型，对应 ChallengeEvent.EventType',
  `payload`      TEXT          NOT NULL COMMENT '序列化的 ChallengeEvent JSON',
  `status`       VARCHAR(16)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING | DELIVERED | FAILED',
  `retry_count`  INT           NOT NULL DEFAULT 0 COMMENT 'relay 已重试次数',
  `created_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `delivered_at` DATETIME      DEFAULT NULL COMMENT '成功投递时间',
  PRIMARY KEY (`id`),
  KEY `idx_outbox_status_created` (`status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事务性发件箱：与业务写同事务，relay 异步投递到 MQ';
