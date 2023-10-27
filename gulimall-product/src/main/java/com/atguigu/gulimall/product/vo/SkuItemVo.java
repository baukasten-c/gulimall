package com.atguigu.gulimall.product.vo;

import com.atguigu.gulimall.product.entity.SkuImagesEntity;
import com.atguigu.gulimall.product.entity.SkuInfoEntity;
import com.atguigu.gulimall.product.entity.SpuInfoDescEntity;
import lombok.Data;

import java.util.List;

@Data
public class SkuItemVo {
    //sku基本信息
    private SkuInfoEntity info;
    //是否有货
    private boolean hasStock = true;
    //sku图片信息
    private List<SkuImagesEntity> images;
    //spu销售属性组合
    private List<SkuItemSaleAttrVo> saleAttrs;
    //spu介绍
    private SpuInfoDescEntity desc;
    //spu规格参数信息
    private List<SpuItemAttrGroupVo> groupAttrs;
}
