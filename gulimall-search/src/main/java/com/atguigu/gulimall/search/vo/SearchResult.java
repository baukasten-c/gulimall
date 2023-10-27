package com.atguigu.gulimall.search.vo;

import com.atguigu.common.to.es.SkuEsModel;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class SearchResult {
    /**
     * 所有查询到的商品信息
     */
    private List<SkuEsModel> product;
    /**
     * 当前页码
     */
    private Integer pageNum;
    /**
     * 总记录数
     */
    private Long total;
    /**
     * 总页码
     */
    private Integer totalPages;
    /**
     * 页码导航
     */
    private List<Integer> pageNavs;
    /**
     * 所有当前查询到的数据涉及到的品牌
     */
    private List<BrandVo> brands;
    /**
     * 所有当前查询到的数据涉及到的分类
     */
    private List<CatelogVo> catelogs;
    /**
     * 所有当前查询到的数据涉及到的属性
     */
    private List<AttrVo> attrs;
    /**
     * 面包屑导航
     */
    private List<NavVo> navs = new ArrayList<>();
    private Set<Long> attrIds = new HashSet<>();

    //参考SkuEsModel进行设计
    @Data
    public static class BrandVo {
        private Long brandId;
        private String brandName;
        private String brandImg;
    }
    @Data
    public static class CatelogVo {
        private Long catelogId;
        private String catelogName;
    }
    @Data
    public static class AttrVo {
        private Long attrId;
        private String attrName;
        private List<String> attrValue;
    }
    @Data
    public static class NavVo {
        private String navName;
        private String navValue;
        private String link;
    }
}
