package com.atguigu.gulimall.product.dao;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author chen
 * @email 946952958@qq.com
 * @date 2023-07-14 15:17:38
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
