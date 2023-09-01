package com.atguigu.gulimall.product.vo;

import lombok.Data;

@Data
public class BrandVo {
    private Long brandId;
    private String brandName;
    public BrandVo(){}
    public BrandVo(Long brandId, String name) {
        this.brandId = brandId;
        this.brandName = name;
    }
}
