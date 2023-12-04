package com.atguigu.common.to;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SpuInfoTo {
    /**
     * 商品id
     */
    @TableId
    private Long spuId;
    /**
     * 商品名称
     */
    private String spuName;
    /**
     * 商品图片
     */
    private String spuPic;
    /**
     * 所属分类id
     */
    private Long catelogId;
    /**
     * 品牌id
     */
    private Long brandId;
    /**
     * 品牌名称
     */
    private String spuBrand;
    /**
     * 商品重量
     */
    private BigDecimal weight;
}
