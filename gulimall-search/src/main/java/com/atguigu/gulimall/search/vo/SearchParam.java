package com.atguigu.gulimall.search.vo;

import lombok.Data;

import java.util.List;

@Data
public class SearchParam { //封装所有页面可能传递过来的查询条件
    /**
     * 全文匹配关键字
     */
    private String keyword;
    /**
     * 三级分类id
     */
    private Long catelog3Id;
    /**
     * 排序条件：sort=price(价格)/salecount(销量)/hotscore(评论分)_desc/asc
     */
    private String sort;
    /**
     * 品牌id,可以多选
     */
    private List<Long> brandId;
    /**
     * 是否显示有货(有：1，无：0)
     */
    private Integer hasStock;
    /**
     * 价格区间：1_500/1_/_500
     */
    private String price;
    /**
     * 商品属性
     */
    private List<String> attrs;
    /**
     * 页码
     */
    private Integer pageNum = 1;
    /**
     * 原生的所有查询条件
     */
    private String queryString;
}
