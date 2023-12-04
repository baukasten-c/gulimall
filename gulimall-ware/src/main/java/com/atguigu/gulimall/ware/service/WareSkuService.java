package com.atguigu.gulimall.ware.service;

import com.atguigu.common.to.OrderTo;
import com.atguigu.common.to.SkuHasStockTo;
import com.atguigu.common.to.WareSkuLockTo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.ware.entity.WareSkuEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author chen
 * @email 946952958@qq.com
 * @date 2023-07-14 17:24:20
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void addStock(Long skuId, Long wareId, Integer skuNum);

    List<SkuHasStockTo> getSkusHasStock(List<Long> skuIds);

    void orderLockStock(WareSkuLockTo to);

    void unlockStock(Long detailId);

    void unlockStock(OrderTo order);
}

