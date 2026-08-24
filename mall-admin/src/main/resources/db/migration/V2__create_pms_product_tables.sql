CREATE TABLE pms_product_category
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    parent_id   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父分类 ID，0 表示一级分类',
    name        VARCHAR(64)     NOT NULL COMMENT '分类名称',
    level       TINYINT         NOT NULL DEFAULT 0 COMMENT '分类层级：0->一级；1->二级',
    sort        INT             NOT NULL DEFAULT 0 COMMENT '排序',
    show_status TINYINT         NOT NULL DEFAULT 1 COMMENT '是否显示：0->不显示；1->显示',
    PRIMARY KEY (id),
    KEY idx_pms_product_category_parent_sort (parent_id, sort),
    KEY idx_pms_product_category_show_sort (show_status, sort)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '商品分类表';

CREATE TABLE pms_product
(
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    brand_id            BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '品牌 ID',
    product_category_id BIGINT UNSIGNED NOT NULL COMMENT '商品分类 ID',
    name                VARCHAR(255)    NOT NULL COMMENT '商品名称',
    product_sn          VARCHAR(64)     NOT NULL COMMENT '货号',
    delete_status       TINYINT         NOT NULL DEFAULT 0 COMMENT '删除状态：0->未删除；1->已删除',
    publish_status      TINYINT         NOT NULL DEFAULT 0 COMMENT '上架状态：0->下架；1->上架',
    verify_status       TINYINT         NOT NULL DEFAULT 0 COMMENT '审核状态：0->未审核；1->审核通过',
    sort                INT             NOT NULL DEFAULT 0 COMMENT '排序',
    price               DECIMAL(10, 2)  NOT NULL DEFAULT 0.00 COMMENT '销售价格',
    stock               INT             NOT NULL DEFAULT 0 COMMENT '库存',
    low_stock           INT             NOT NULL DEFAULT 0 COMMENT '库存预警值',
    pic                 VARCHAR(500)    DEFAULT NULL COMMENT '商品主图',
    detail_desc         TEXT            COMMENT '商品详情描述',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pms_product_product_sn (product_sn),
    KEY idx_pms_product_category_status_sort (product_category_id, delete_status, publish_status, sort),
    KEY idx_pms_product_brand_id (brand_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '商品表';

CREATE TABLE pms_sku_stock
(
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    product_id BIGINT UNSIGNED NOT NULL COMMENT '商品 ID',
    sku_code   VARCHAR(64)     NOT NULL COMMENT 'SKU 编码',
    price      DECIMAL(10, 2)  NOT NULL DEFAULT 0.00 COMMENT 'SKU 价格',
    stock      INT             NOT NULL DEFAULT 0 COMMENT 'SKU 库存',
    pic        VARCHAR(500)    DEFAULT NULL COMMENT 'SKU 图片',
    sale_attrs VARCHAR(500)    DEFAULT NULL COMMENT '销售属性 JSON',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pms_sku_stock_product_sku_code (product_id, sku_code),
    KEY idx_pms_sku_stock_product_id (product_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '商品 SKU 库存表';