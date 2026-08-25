ALTER TABLE oms_cart_item
    ADD COLUMN product_sku_attrs VARCHAR(500) DEFAULT NULL
    COMMENT 'SKU 销售属性快照'
        AFTER product_sku_code;