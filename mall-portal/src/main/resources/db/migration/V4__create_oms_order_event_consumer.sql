CREATE TABLE oms_order_event_consumer
(
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    event_id      CHAR(36)        NOT NULL COMMENT 'Admin 确认事件 ID',
    consumer_name VARCHAR(64)     NOT NULL COMMENT '消费者名称',
    order_id      BIGINT UNSIGNED NOT NULL COMMENT 'Portal 订单主键',
    order_sn      VARCHAR(64)     NOT NULL COMMENT '订单编号',
    create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_oms_order_event_consumer_event
        (event_id, consumer_name),
    KEY idx_oms_order_event_consumer_order
        (order_id, order_sn)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'Portal 订单事件消费幂等日志';