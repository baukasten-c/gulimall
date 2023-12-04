package com.atguigu.gulimall.product.dao;

import com.atguigu.gulimall.product.entity.SkuInfoEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * sku信息
 * 
 * @author chen
 * @email 946952958@qq.com
 * @date 2023-07-14 15:17:38
 */
@Mapper
public interface SkuInfoDao extends BaseMapper<SkuInfoEntity> {
    @MapKey("skuId")
    Map<Long, Map<String, Object>> getPricesByIds(List<Long> skuIds);
}
