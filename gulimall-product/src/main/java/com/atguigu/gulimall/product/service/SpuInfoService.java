package com.atguigu.gulimall.product.service;

import com.atguigu.common.to.SpuInfoTo;
import com.atguigu.gulimall.product.vo.SpuSaveVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.SpuInfoEntity;

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
public interface SpuInfoService extends IService<SpuInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveSpuInfo(SpuSaveVo spuSaveVo);

    PageUtils queryPageByCondition(Map<String, Object> params);

    void up(Long spuId);

    Map<Long, Map<String, BigDecimal>> getWeightsByIds(List<Long> spuIds);

    SpuInfoTo getSpuInfoBySkuId(Long skuId);
}

