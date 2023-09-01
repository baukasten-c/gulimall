/**
 * Copyright 2019 bejson.com
 */
package com.atguigu.gulimall.product.vo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * Auto-generated: 2019-11-26 10:50:34
 *
 * @author bejson.com (i@bejson.com)
 * @website http://www.bejson.com/java2pojo/
 */
@Data
public class SpuSaveVo {
    @NotBlank(message = "商品名称不能为空")
    private String spuName;
    @NotBlank(message = "简单描述不能为空")
    private String spuDescription;
    @NotNull(message = "分类不能为空")
    private Long catelogId;
    @NotNull(message = "品牌不能为空")
    private Long brandId;
    @NotNull(message = "重量值不能为空")
    private BigDecimal weight;
    private int publishStatus;
    @NotEmpty(message = "商品详情图集不能为空")
    private List<String> decript;
    @NotEmpty(message = "商品图片集不能为空")
    private List<String> images;
    private Bounds bounds;
    private List<BaseAttrs> baseAttrs;
    private List<Skus> skus;
}