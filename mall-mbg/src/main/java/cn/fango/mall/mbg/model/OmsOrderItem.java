package cn.fango.mall.mbg.model;

import java.math.BigDecimal;
import java.util.Date;

public class OmsOrderItem {
    private Long id;

    private Long orderId;

    private Long productId;

    private String productName;

    private String productPic;

    private Long productSkuId;

    private String productSkuCode;

    private String productSkuAttrs;

    private BigDecimal productPrice;

    private Integer productQuantity;

    private BigDecimal productTotalAmount;

    private Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName == null ? null : productName.trim();
    }

    public String getProductPic() {
        return productPic;
    }

    public void setProductPic(String productPic) {
        this.productPic = productPic == null ? null : productPic.trim();
    }

    public Long getProductSkuId() {
        return productSkuId;
    }

    public void setProductSkuId(Long productSkuId) {
        this.productSkuId = productSkuId;
    }

    public String getProductSkuCode() {
        return productSkuCode;
    }

    public void setProductSkuCode(String productSkuCode) {
        this.productSkuCode = productSkuCode == null ? null : productSkuCode.trim();
    }

    public String getProductSkuAttrs() {
        return productSkuAttrs;
    }

    public void setProductSkuAttrs(String productSkuAttrs) {
        this.productSkuAttrs = productSkuAttrs == null ? null : productSkuAttrs.trim();
    }

    public BigDecimal getProductPrice() {
        return productPrice;
    }

    public void setProductPrice(BigDecimal productPrice) {
        this.productPrice = productPrice;
    }

    public Integer getProductQuantity() {
        return productQuantity;
    }

    public void setProductQuantity(Integer productQuantity) {
        this.productQuantity = productQuantity;
    }

    public BigDecimal getProductTotalAmount() {
        return productTotalAmount;
    }

    public void setProductTotalAmount(BigDecimal productTotalAmount) {
        this.productTotalAmount = productTotalAmount;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}