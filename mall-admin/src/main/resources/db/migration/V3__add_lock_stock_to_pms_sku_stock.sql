ALTER TABLE pms_sku_stock
    ADD COLUMN lock_stock INT NOT NULL DEFAULT 0 COMMENT '锁定库存' AFTER stock;