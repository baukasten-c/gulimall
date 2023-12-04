package com.atguigu.gulimall.product.controller;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.atguigu.common.to.SpuInfoTo;
import com.atguigu.gulimall.product.vo.SpuSaveVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.atguigu.gulimall.product.entity.SpuInfoEntity;
import com.atguigu.gulimall.product.service.SpuInfoService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.R;



/**
 * spu信息
 *
 * @author chen
 * @email 946952958@qq.com
 * @date 2023-07-14 15:17:39
 */
@RestController
@RequestMapping("product/spuinfo")
public class SpuInfoController {
    @Autowired
    private SpuInfoService spuInfoService;

    //获取商品重量
    @GetMapping("/weights/{spuIds}")
    public Map<Long, Map<String, BigDecimal>> getWeights(@PathVariable("spuIds") List<Long> spuIds){
        return spuInfoService.getWeightsByIds(spuIds);
    }

    //根据skuId查询spu的信息
    @GetMapping("/skuId/{skuId}")
    public R getSpuInfoBySkuId(@PathVariable("skuId") Long skuId){
        SpuInfoTo spuInfoTo = spuInfoService.getSpuInfoBySkuId(skuId);
        return R.ok().put("spuInfo", spuInfoTo);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){ //分页查询参数用Map接收，其他的查询用Vo或者Entity接收
        PageUtils page = spuInfoService.queryPageByCondition(params);
        return R.ok().put("page", page);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
		SpuInfoEntity spuInfo = spuInfoService.getById(id);
        return R.ok().put("spuInfo", spuInfo);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@Validated @RequestBody SpuSaveVo spuSaveVo){
		spuInfoService.saveSpuInfo(spuSaveVo);
        return R.ok();
    }

    //商品上架
    @PostMapping("/{spuId}/up")
    public R spuUp(@PathVariable("spuId") Long spuId){
        spuInfoService.up(spuId);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody SpuInfoEntity spuInfo){
		spuInfoService.updateById(spuInfo);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids){
		spuInfoService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
