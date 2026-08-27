CREATE TABLE pms_outbox_event
(
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    event_id      CHAR(36)        NOT NULL COMMENT '对外事件 ID，UUID',
    aggregate_type VARCHAR(32)    NOT NULL COMMENT '聚合类型，例如 PRODUCT',
    aggregate_id  BIGINT UNSIGNED NOT NULL COMMENT '聚合主键，例如商品 ID',
    event_type    VARCHAR(64)     NOT NULL COMMENT '事件类型，例如 PRODUCT_CHANGED',
    payload       LONGTEXT        NOT NULL COMMENT '事件 JSON 载荷',
    status        VARCHAR(16)     NOT NULL COMMENT '发布状态：PENDING、PUBLISHED',
    retry_count   INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '发布尝试次数',
    next_retry_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下次允许发布的时间',
    published_at  DATETIME        DEFAULT NULL COMMENT '确认发布成功时间',
    last_error    VARCHAR(500)   DEFAULT NULL COMMENT '最近一次发布失败原因',
    create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pms_outbox_event_event_id (event_id),
    KEY idx_pms_outbox_event_status_retry (status, next_retry_at),
    KEY idx_pms_outbox_event_aggregate (aggregate_type, aggregate_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '商品事务外盒事件表';