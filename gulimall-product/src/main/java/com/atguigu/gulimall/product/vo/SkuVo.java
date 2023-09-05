package com.atguigu.gulimall.product.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SkuVo {
    private Long skuId;
    private String skuName;
    private BigDecimal price;
    public SkuVo() {}
    public SkuVo(Long skuId, String skuName, BigDecimal price) {
        this.skuId = skuId;
        this.skuName = skuName;
        this.price = price;
    }
}
