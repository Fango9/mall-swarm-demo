ALTER TABLE pms_stock_reservation
    ADD COLUMN expire_at DATETIME DEFAULT NULL COMMENT 'LOCKED 状态的过期时间' AFTER status,
    MODIFY COLUMN status VARCHAR(16) NOT NULL
        COMMENT '预占状态：LOCKED、CONFIRMED、RELEASED',
    ADD KEY idx_pms_stock_reservation_status_expire_at (status, expire_at);

CREATE TABLE pms_event_consume_log
(
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    event_id      CHAR(36)        NOT NULL COMMENT '已消费的 Outbox 事件 ID',
    consumer      VARCHAR(64)     NOT NULL COMMENT '消费者标识',
    create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pms_event_consume_log_event_consumer (event_id, consumer)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '消息消费幂等记录表';