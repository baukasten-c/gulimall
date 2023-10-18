package com.atguigu.gulimall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

//二级分类Vo
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Catelog2Vo {
    private String catelog1Id; //一级父分类id
    private List<Catelog3Vo> catelog3List; //三级子分类
    private String id;
    private String name;

    //三级分类Vo
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Catelog3Vo{
        private String catelog2Id; //二级父分类id
        private String id;
        private String name;
    }
}
