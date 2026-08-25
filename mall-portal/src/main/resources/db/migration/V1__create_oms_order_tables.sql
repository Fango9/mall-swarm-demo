CREATE TABLE oms_cart_item
(
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    member_id      BIGINT UNSIGNED NOT NULL COMMENT '会员 ID',
    product_id     BIGINT UNSIGNED NOT NULL COMMENT '商品 ID',
    product_name   VARCHAR(255)    NOT NULL COMMENT '商品名称快照',
    product_pic    VARCHAR(500)    DEFAULT NULL COMMENT '商品图片快照',
    product_sku_id BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID',
    product_sku_code VARCHAR(64)   NOT NULL COMMENT 'SKU 编码快照',
    price          DECIMAL(10, 2)  NOT NULL COMMENT 'SKU 单价快照',
    quantity       INT             NOT NULL COMMENT '购买数量',
    create_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_oms_cart_item_member_sku (member_id, product_sku_id),
    KEY idx_oms_cart_item_member_modify_time (member_id, modify_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '购物车项表';

CREATE TABLE oms_order
(
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_sn        VARCHAR(64)     NOT NULL COMMENT '订单编号',
    member_id       BIGINT UNSIGNED NOT NULL COMMENT '会员 ID',
    idempotency_key VARCHAR(128)    NOT NULL COMMENT '下单幂等键',
    status          VARCHAR(32)     NOT NULL COMMENT '订单状态',
    total_amount    DECIMAL(10, 2)  NOT NULL COMMENT '订单总金额',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_oms_order_order_sn (order_sn),
    UNIQUE KEY uk_oms_order_member_idempotency_key (member_id, idempotency_key),
    KEY idx_oms_order_member_create_time (member_id, create_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '订单表';

CREATE TABLE oms_order_item
(
    id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id           BIGINT UNSIGNED NOT NULL COMMENT '订单 ID',
    product_id         BIGINT UNSIGNED NOT NULL COMMENT '商品 ID',
    product_name       VARCHAR(255)    NOT NULL COMMENT '商品名称快照',
    product_pic        VARCHAR(500)    DEFAULT NULL COMMENT '商品图片快照',
    product_sku_id     BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID',
    product_sku_code   VARCHAR(64)     NOT NULL COMMENT 'SKU 编码快照',
    product_sku_attrs  VARCHAR(500)    DEFAULT NULL COMMENT 'SKU 销售属性快照',
    product_price      DECIMAL(10, 2)  NOT NULL COMMENT '商品单价快照',
    product_quantity   INT             NOT NULL COMMENT '购买数量',
    product_total_amount DECIMAL(10, 2) NOT NULL COMMENT '明细总金额',
    create_time        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_oms_order_item_order_id (order_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '订单明细表';