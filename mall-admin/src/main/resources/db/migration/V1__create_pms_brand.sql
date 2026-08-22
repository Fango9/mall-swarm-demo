CREATE TABLE pms_brand
(
    id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    name                  VARCHAR(64)     NOT NULL COMMENT '品牌名称',
    first_letter          VARCHAR(8)      DEFAULT NULL COMMENT '首字母',
    sort                  INT             NOT NULL DEFAULT 0 COMMENT '排序',
    factory_status        TINYINT         NOT NULL DEFAULT 0 COMMENT '是否为品牌制造商：0->不是；1->是',
    show_status           TINYINT         NOT NULL DEFAULT 1 COMMENT '是否显示：0->不显示；1->显示',
    product_count         INT             NOT NULL DEFAULT 0 COMMENT '产品数量',
    product_comment_count INT             NOT NULL DEFAULT 0 COMMENT '产品评论数量',
    logo                  VARCHAR(255)    DEFAULT NULL COMMENT '品牌 Logo',
    big_pic               VARCHAR(255)    DEFAULT NULL COMMENT '专区大图',
    brand_story           TEXT            COMMENT '品牌故事',
    PRIMARY KEY (id),
    KEY idx_pms_brand_show_sort (show_status, sort)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '商品品牌表';