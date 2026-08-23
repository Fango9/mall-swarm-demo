CREATE TABLE ums_member
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(64)     NOT NULL COMMENT '登录用户名',
    password    VARCHAR(100)    NOT NULL COMMENT 'BCrypt 密码哈希',
    role        VARCHAR(32)     NOT NULL DEFAULT 'MEMBER' COMMENT '角色：MEMBER、ADMIN',
    status      TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：0->禁用；1->启用',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ums_member_username (username),
    KEY idx_ums_member_role_status (role, status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '会员最小身份表';