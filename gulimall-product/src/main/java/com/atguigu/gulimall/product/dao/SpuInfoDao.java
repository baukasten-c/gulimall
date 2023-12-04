package com.atguigu.gulimall.product.dao;

import com.atguigu.gulimall.product.entity.SpuInfoEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * spu信息
 * 
 * @author chen
 * @email 946952958@qq.com
 * @date 2023-07-14 15:17:39
 */
@Mapper
public interface SpuInfoDao extends BaseMapper<SpuInfoEntity> {
    @MapKey("spuId")
    Map<Long, Map<String, BigDecimal>> getWeightsByIds(List<Long> spuIds);
}
