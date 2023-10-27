package com.atguigu.common.to.es;

import jdk.internal.util.xml.impl.Attrs;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SkuEsModel {
    private Long skuId;
    private Long spuId;
    private String skuTitle;
    private BigDecimal price;
    private String skuDefaultImg;
    private Long saleCount;
    private Boolean hasStock;
    private Long hotScore;
    private Long brandId;
    private String brandName;
    private String brandImg;
    private Long catelogId;
    private String catelogName;
    private List<Attr> attrs;
    @Data
    public static class Attr {
        private Long attrId;
        private String attrName;
        private List<String> attrValue;
    }
}
