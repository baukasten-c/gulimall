package com.atguigu.gulimall.ware.service.impl;

import com.atguigu.common.constant.WareConstant;
import com.atguigu.gulimall.ware.entity.PurchaseDetailEntity;
import com.atguigu.gulimall.ware.service.PurchaseDetailService;
import com.atguigu.gulimall.ware.service.WareSkuService;
import com.atguigu.gulimall.ware.vo.MergeVo;
import com.atguigu.gulimall.ware.vo.PurchaseDoneVo;
import com.atguigu.gulimall.ware.vo.PurchaseItemDoneVo;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.ware.dao.PurchaseDao;
import com.atguigu.gulimall.ware.entity.PurchaseEntity;
import com.atguigu.gulimall.ware.service.PurchaseService;
import org.springframework.transaction.annotation.Transactional;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {
    @Autowired
    PurchaseDetailService purchaseDetailService;
    @Autowired
    WareSkuService wareSkuService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>()
        );

        return new PageUtils(page);
    }

    //查询未领取的采购单
    @Override
    public List<PurchaseEntity> queryPageUnreceive() {
        List<PurchaseEntity> purchaseEntities = this.list(new QueryWrapper<PurchaseEntity>().eq("status", 0).or().eq("status", 1));
        return purchaseEntities;
    }

    //合并采购需求
    @Override
    @Transactional
    public void mergePurchase(MergeVo mergeVo) {
        Long purchaseId = mergeVo.getPurchaseId();
        if(purchaseId == null){
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            purchaseEntity.setPriority(1);
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.CREATED.getCode());
            this.save(purchaseEntity);
            purchaseId = purchaseEntity.getId();
        }
        //采购单状态是”新建/已分配“才可以合并
        int purchaseStatus = this.getById(purchaseId).getStatus();
        if(purchaseStatus == WareConstant.PurchaseStatusEnum.CREATED.getCode() ||
                purchaseStatus == WareConstant.PurchaseStatusEnum.ASSIGNED.getCode()){
            List<Long> items = mergeVo.getItems();
            BigDecimal amount = BigDecimal.ZERO;
            List<PurchaseDetailEntity> purchaseDetailEntities = new ArrayList<>();
            for(PurchaseDetailEntity entity : purchaseDetailService.listByIds(items)){
                if(entity.getStatus() == WareConstant.PurchaseDetailStatusEnum.CREATED.getCode()){
                    entity.setPurchaseId(purchaseId);
                    entity.setStatus(WareConstant.PurchaseDetailStatusEnum.ASSIGNED.getCode());
                    amount = amount.add(entity.getSkuPrice());
                    purchaseDetailEntities.add(entity);
                }
            }
            if(!purchaseDetailEntities.isEmpty()){
                purchaseDetailService.updateBatchById(purchaseDetailEntities);
                //更新采购单的金额
                //PurchaseEntity purchaseEntity = this.getById(purchaseId); 查出来的数据无法自动填充更新时间
                PurchaseEntity purchaseEntity = new PurchaseEntity();
                purchaseEntity.setId(purchaseId);
                purchaseEntity.setAmount(amount);
                this.updateById(purchaseEntity);
            }
        }
    }

    //领取采购单
    @Override
    @Transactional
    public void received(List<Long> ids) {
        //确认当前采购单是已分配状态
        QueryWrapper<PurchaseEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("status", WareConstant.PurchaseStatusEnum.ASSIGNED.getCode()).in("id", ids);
        List<PurchaseEntity> purchaseEntities = this.list(wrapper);
        if(!purchaseEntities.isEmpty()){
            //改变采购单的状态
            purchaseEntities = purchaseEntities.stream().map(entity -> {
                PurchaseEntity purchaseEntity = new PurchaseEntity();
                purchaseEntity.setId(entity.getId());
                purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.RECEIVE.getCode());
                return purchaseEntity;
            }).collect(Collectors.toList());
            this.updateBatchById(purchaseEntities);
            //改变采购需求的状态
            purchaseEntities.forEach(purchaseEntity -> {
                List<PurchaseDetailEntity> purchaseDetailEntities = purchaseDetailService.listDetailByPurchaseId(purchaseEntity.getId())
                        .stream().peek(entity -> entity.setStatus(WareConstant.PurchaseDetailStatusEnum.BUYING.getCode()))
                        .collect(Collectors.toList());
                purchaseDetailService.updateBatchById(purchaseDetailEntities);
            });
        }
    }

    //完成采购
    @Override
    @Transactional
    public void done(PurchaseDoneVo purchaseDoneVo) {
        Long id = purchaseDoneVo.getId();
        //改变采购需求的状态
        Boolean flag = true;
        List<PurchaseItemDoneVo> items = purchaseDoneVo.getItems();
        List<PurchaseDetailEntity> purchaseDetailEntities = new ArrayList<>();
        for (PurchaseItemDoneVo item : items) {
            PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
            purchaseDetailEntity.setId(item.getItemId());
            if(item.getStatus() == WareConstant.PurchaseDetailStatusEnum.HASERROR.getCode()){
                flag = false;
                purchaseDetailEntity.setStatus(item.getStatus());
                purchaseDetailEntity.setReason(item.getReason());
            }else{
                purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.FINISH.getCode());
                //将成功的采购需求入库
                PurchaseDetailEntity entity = purchaseDetailService.getById(item.getItemId());
                wareSkuService.addStock(entity.getSkuId(), entity.getWareId(), entity.getSkuNum());
            }
            purchaseDetailEntities.add(purchaseDetailEntity);
        }
        purchaseDetailService.updateBatchById(purchaseDetailEntities);
        //改变采购单状态
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(id);
        purchaseEntity.setStatus(flag ? WareConstant.PurchaseStatusEnum.FINISH.getCode() :
                WareConstant.PurchaseStatusEnum.HASERROR.getCode());
        this.updateById(purchaseEntity);
    }
}