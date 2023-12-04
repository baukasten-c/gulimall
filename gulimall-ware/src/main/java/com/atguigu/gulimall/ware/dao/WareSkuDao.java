package com.atguigu.gulimall.ware.dao;

import com.atguigu.gulimall.ware.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 商品库存
 * 
 * @author chen
 * @email 946952958@qq.com
 * @date 2023-07-14 17:24:20
 */
@Mapper
public interface WareSkuDao extends BaseMapper<WareSkuEntity> {

    void addStock(@Param("skuId") Long skuId, @Param("wareId") Long wareId, @Param("skuNum") Integer skuNum);

    @MapKey("skuId")
    Map<Long, Map> getSkuStocks(List<Long> skuIds);

    List<Long> listWareIdHasSkuStock(@Param("skuId") Long skuId, @Param("num") Integer num);

    Long lockSkuStock(@Param("skuId") Long skuId, @Param("wareId") Long wareId, @Param("num") Integer num);

    Long unlockSkuStock(@Param("skuId") Long skuId, @Param("wareId") Long wareId, @Param("num") Integer num);
}
