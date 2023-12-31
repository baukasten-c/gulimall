package com.atguigu.gulimall.cart.feign;

import com.atguigu.common.to.SpuInfoTo;
import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@FeignClient("gulimall-product")
public interface ProductFeignService {
    @RequestMapping("/product/skuinfo/info/{skuId}")
    R info(@PathVariable("skuId") Long skuId);
    @GetMapping("/product/skusaleattrvalue/stringlist/{skuId}")
    List<String> getSkuSaleAttrValuesAsList(@PathVariable("skuId") Long skuId);
    @GetMapping("/product/skuinfo/prices/{skuIds}")
    Map<Long, Map<String, Object>> getPrices(@PathVariable("skuIds") List<Long> skuIds);
    @GetMapping("/product/spuinfo/weights/{spuIds}")
    Map<Long, Map<String, BigDecimal>> getWeights(@PathVariable("spuIds") List<Long> spuIds);
}
