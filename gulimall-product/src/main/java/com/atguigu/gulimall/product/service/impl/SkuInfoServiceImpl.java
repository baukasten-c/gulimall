package com.atguigu.gulimall.product.service.impl;

import com.atguigu.gulimall.product.config.ThreadPoolConfigProperties;
import com.atguigu.gulimall.product.entity.SkuImagesEntity;
import com.atguigu.gulimall.product.entity.SpuInfoDescEntity;
import com.atguigu.gulimall.product.service.*;
import com.atguigu.gulimall.product.vo.Attr;
import com.atguigu.gulimall.product.vo.SkuItemSaleAttrVo;
import com.atguigu.gulimall.product.vo.SkuItemVo;
import com.atguigu.gulimall.product.vo.SpuItemAttrGroupVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.SkuInfoDao;
import com.atguigu.gulimall.product.entity.SkuInfoEntity;
import org.springframework.util.StringUtils;

import static java.awt.SystemColor.info;


@Service("skuInfoService")
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {
    @Autowired
    private SkuImagesService skuImagesService;
    @Autowired
    private SpuInfoDescService spuInfoDescService;
    @Autowired
    private AttrGroupService attrGroupService;
    @Autowired
    private SkuSaleAttrValueService skuSaleAttrValueService;
    @Autowired
    private ThreadPoolExecutor executor;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                new QueryWrapper<SkuInfoEntity>()
        );
        return new PageUtils(page);
    }

    //sku检索
    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SkuInfoEntity> wrapper = new QueryWrapper<>();
        String catelogId = (String) params.get("catelogId");
        if(!StringUtils.isEmpty(catelogId) && !"0".equalsIgnoreCase(catelogId)){
            wrapper.eq("catelog_id", catelogId);
        }
        String brandId = (String) params.get("brandId");
        if(!StringUtils.isEmpty(brandId) && !"0".equalsIgnoreCase(brandId)){
            wrapper.eq("brand_id", brandId);
        }
        String min = (String) params.get("min");
        if(!StringUtils.isEmpty(min)){
            try{
                //避免min包含非数字字符
                BigDecimal bigDecimal = new BigDecimal(min);
                if(bigDecimal.compareTo(new BigDecimal("0")) == 1){
                    wrapper.ge("price", min);
                }
            }catch (Exception e){}
        }
        String max = (String) params.get("max");
        if(!StringUtils.isEmpty(max)){
            try{
                //避免max包含非数字字符
                BigDecimal bigDecimal = new BigDecimal(max);
                if(bigDecimal.compareTo(new BigDecimal("0")) == 1){
                    wrapper.le("price", max);
                }
            }catch (Exception e){}
        }
        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)){
            wrapper.and(w -> w.like("sku_id", key).or().like("sku_name", key));
        }
        IPage<SkuInfoEntity> page = this.page(new Query<SkuInfoEntity>().getPage(params), wrapper);
        return new PageUtils(page);
    }

    //根据spuId获取sku信息
    @Override
    public List<SkuInfoEntity> getSkusBySpuId(Long spuId) {
        List<SkuInfoEntity> skuInfoEntities = this.list(new QueryWrapper<SkuInfoEntity>().eq("spu_id", spuId));
        return skuInfoEntities;
    }

    //获取sku详情用于页面展示
    @Override
    public SkuItemVo item(Long skuId) throws ExecutionException, InterruptedException {
        SkuItemVo skuItemVo = new SkuItemVo();
        //sku基本信息  pms_sku_info
        CompletableFuture<SkuInfoEntity> infoFuture = CompletableFuture.supplyAsync(() -> {
            SkuInfoEntity info = getById(skuId);
            skuItemVo.setInfo(info);
            return info;
        }, executor);
        //spu介绍  pms_spu_info_desc
        CompletableFuture<Void> descFuture = infoFuture.thenAcceptAsync(res -> {
            SpuInfoDescEntity desc = spuInfoDescService.getById(res.getSpuId());
            skuItemVo.setDesc(desc);
        }, executor);
        //spu销售属性组合
        CompletableFuture<Void> saleAttrsFuture = infoFuture.thenAcceptAsync(res -> {
            List<SkuItemSaleAttrVo> saleAttrs = skuSaleAttrValueService.getSaleAttrBySpuId(res.getSpuId());
            skuItemVo.setSaleAttrs(saleAttrs);
        }, executor);
        //spu规格参数信息
        CompletableFuture<Void> groupAttrsFuture = infoFuture.thenAcceptAsync(res -> {
            List<SpuItemAttrGroupVo> groupAttrs = attrGroupService.getAttrGroupWithAttrsBySpuId(res.getSpuId(), res.getCatelogId());
            skuItemVo.setGroupAttrs(groupAttrs);
        }, executor);
        //sku图片信息  pms_sku_images
        CompletableFuture<Void> imagesFuture = CompletableFuture.runAsync(() -> {
            List<SkuImagesEntity> images = skuImagesService.getImagesBySkuId(skuId);
            skuItemVo.setImages(images);
        }, executor);
        //等待全部任务完成
        CompletableFuture.allOf(descFuture, saleAttrsFuture, groupAttrsFuture, imagesFuture).get();
        return skuItemVo;
    }

    //获取商品最新价格
    @Override
    public Map<Long, Map<String, Object>> getPricesByIds(List<Long> skuIds) {
        return baseMapper.getPricesByIds(skuIds);
    }
}