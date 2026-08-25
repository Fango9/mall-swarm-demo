CREATE TABLE pms_stock_reservation
(
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    reservation_no VARCHAR(64)     NOT NULL COMMENT '库存预占编号，使用订单编号',
    sku_id         BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID',
    quantity       INT             NOT NULL COMMENT '预占数量',
    status         VARCHAR(16)     NOT NULL COMMENT '预占状态：LOCKED、RELEASED',
    create_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pms_stock_reservation_no_sku (reservation_no, sku_id),
    KEY idx_pms_stock_reservation_no_status (reservation_no, status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'SKU 库存预占记录表';