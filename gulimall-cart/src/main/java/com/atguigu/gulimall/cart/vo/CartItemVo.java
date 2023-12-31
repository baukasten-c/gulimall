package com.atguigu.gulimall.cart.vo;

import java.math.BigDecimal;
import java.util.List;

public class CartItemVo { //单个购物项
    private Long skuId;
    private Boolean check = true;
    private String title;
    private String image;
    /**
     * 商品套餐属性
     */
    private List<String> skuAttrValues;
    private BigDecimal price;
    private Integer count;
    private BigDecimal totalPrice;
    private BigDecimal weight;

    public Long getSkuId() {
        return skuId;
    }
    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }
    public Boolean getCheck() {
        return check;
    }
    public void setCheck(Boolean check) {
        this.check = check;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getImage() {
        return image;
    }
    public void setImage(String image) {
        this.image = image;
    }
    public List<String> getSkuAttrValues() {
        return skuAttrValues;
    }
    public void setSkuAttrValues(List<String> skuAttrValues) {
        this.skuAttrValues = skuAttrValues;
    }
    public BigDecimal getPrice() {
        return price;
    }
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    public Integer getCount() {
        return count;
    }
    public void setCount(Integer count) {
        this.count = count;
    }
    public BigDecimal getTotalPrice() { //计算当前购物项总价
        //BigDecimal除了字符串，其他的数据类型都会丢失精度
        return this.price.multiply(new BigDecimal("" + this.count));
    }
    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }
    public BigDecimal getWeight() {
        return weight;
    }
    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }
}
