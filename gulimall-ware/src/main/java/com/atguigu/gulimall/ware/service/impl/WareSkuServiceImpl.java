package com.atguigu.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.constant.OrderConstant;
import com.atguigu.common.constant.WareConstant;
import com.atguigu.common.to.OrderItemTo;
import com.atguigu.common.to.OrderTo;
import com.atguigu.common.to.WareSkuLockTo;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.atguigu.gulimall.ware.entity.WareOrderTaskEntity;
import com.atguigu.gulimall.ware.exception.NoStockException;
import com.atguigu.gulimall.ware.feign.OrderFeignService;
import com.atguigu.gulimall.ware.feign.ProductFeignService;
import com.atguigu.common.to.SkuHasStockTo;
import com.atguigu.gulimall.ware.service.WareOrderTaskDetailService;
import com.atguigu.gulimall.ware.service.WareOrderTaskService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.ware.dao.WareSkuDao;
import com.atguigu.gulimall.ware.entity.WareSkuEntity;
import com.atguigu.gulimall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {
    @Autowired
    private WareSkuDao wareSkuDao;
    @Autowired
    private ProductFeignService productFeignService;
    @Autowired
    private WareOrderTaskService wareOrderTaskService;
    @Autowired
    private WareOrderTaskDetailService wareOrderTaskDetailService;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private OrderFeignService orderFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();
        String wareId = (String) params.get("wareId");
        if(!StringUtils.isEmpty(wareId)){
            wrapper.eq("ware_id", wareId);
        }
        String skuId = (String) params.get("skuId");
        if(!StringUtils.isEmpty(skuId)){
            wrapper.like("sku_id", skuId);
        }
        IPage<WareSkuEntity> page = this.page(new Query<WareSkuEntity>().getPage(params), wrapper);
        return new PageUtils(page);
    }

    //入库
    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        WareSkuEntity wareSkuEntity = wareSkuDao.selectOne(
                new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId)
        );
        //判断，如果还没有库存记录，新增
        if(wareSkuEntity == null){
            WareSkuEntity skuEntity = new WareSkuEntity();
            skuEntity.setSkuId(skuId);
            skuEntity.setWareId(wareId);
            skuEntity.setStock(skuNum);
            //远程查询sku的名字，即使失败，整个事务也无需回滚
            //方法一：catch异常
            try {
                R r = productFeignService.info(skuId);
                Map<String,Object> data = (Map<String, Object>) r.get("skuInfo");
                if(r.getCode() == 0){ //成功
                    skuEntity.setSkuName((String) data.get("skuName"));
                }
            }catch (Exception e){}
            skuEntity.setStockLocked(0);
            wareSkuDao.insert(skuEntity);
        }else{ //否则，修改
            wareSkuDao.addStock(skuId, wareId, skuNum);
        }
    }

    //查询sku是否有库存
    @Override
    @Transactional
    public List<SkuHasStockTo> getSkusHasStock(List<Long> skuIds) {
        Map<Long, Map> skuStocks = baseMapper.getSkuStocks(skuIds);
        List<SkuHasStockTo> skuHasStockTos = new ArrayList<>();
        for (Long skuId : skuIds) {
            boolean hasStock = false;
            if(skuStocks.containsKey(skuId)){
                BigDecimal stock = (BigDecimal) skuStocks.get(skuId).get("stock");
                hasStock = stock != null && stock.intValue() > 0;
            }
            SkuHasStockTo skuHasStockTo = new SkuHasStockTo(skuId, hasStock);
            skuHasStockTos.add(skuHasStockTo);
        }
        return skuHasStockTos;
    }

    //锁定库存
    @Override
    @Transactional
    public void orderLockStock(WareSkuLockTo to) {
        //保存库存工作单
        WareOrderTaskEntity wareOrderTaskEntity = new WareOrderTaskEntity();
        wareOrderTaskEntity.setOrderSn(to.getOrderSn());
        wareOrderTaskService.save(wareOrderTaskEntity);
        //查询库存
        List<OrderItemTo> lockItems = to.getLocks();
        lockItems.forEach(item -> {
            //遍历订单项，查询有库存的仓库
            Long skuId = item.getSkuId();
            Integer num = item.getCount();
            List<Long> wareIds = wareSkuDao.listWareIdHasSkuStock(skuId, num);
            if(CollectionUtils.isEmpty(wareIds)){
                throw new NoStockException(skuId);
            }
            //锁定库存
            boolean skuStocked = false;
            for(Long wareId : wareIds){
                if(wareSkuDao.lockSkuStock(skuId, wareId, num) == 1){
                    skuStocked = true;
                    Long taskId = wareOrderTaskEntity.getId();
                    //锁定成功，保存库存工作单详情
                    WareOrderTaskDetailEntity taskDetailEntity = new WareOrderTaskDetailEntity(null,
                            skuId, item.getSkuName(), num, taskId, wareId, 1);
                    wareOrderTaskDetailService.save(taskDetailEntity);
                    //库存工作单详情放入延迟队列(不需要传完整工作单详情)，消息确认机制保证消息不会发送失败
                    rabbitTemplate.convertAndSend(WareConstant.STOCK_EXCHANGE,
                            WareConstant.STOCK_DELAY_QUEUE_KEY, taskDetailEntity.getId());
                    break;
                }
            }
            //锁定失败，回滚
            if(!skuStocked){
                throw new NoStockException(skuId);
            }
        });
    }

    //解锁库存(被动)
    public void unlockStock(Long detailId){
        WareOrderTaskDetailEntity taskDetailEntity = wareOrderTaskDetailService.getById(detailId);
        //taskDetailEntity为null，说明有商品库存不足，已自动回滚，不需要解锁库存
        if(taskDetailEntity != null){
            WareOrderTaskEntity taskEntity = wareOrderTaskService.getById(taskDetailEntity.getTaskId());
            String orderSn = taskEntity.getOrderSn();
            R r = orderFeignService.getOrderStatus(orderSn);
            //远程查询订单信息
            if(r.getCode() == 0){
                //获取订单状态
                OrderTo orderInfo = r.getData("data", new TypeReference<OrderTo>() {});
                //当订单不存在或订单已取消，且库存未解锁过时，才需要解锁库存
                if((orderInfo == null || orderInfo.getStatus() == OrderConstant.OrderStatusEnum.CANCLED.getCode()) && taskDetailEntity.getLockStatus() == 1){
                    orderUnlockStock(taskDetailEntity.getSkuId(), taskDetailEntity.getWareId(), taskDetailEntity.getSkuNum(), detailId);
                }
            }else{
                throw new RuntimeException("远程调用服务失败");
            }
        }
    }

    //解锁库存
    @Transactional
    public void orderUnlockStock(Long skuId, Long wareId, Integer num, Long detailId){
        //解锁库存，更新失败MySQL会返回适当的错误码和错误信息
        wareSkuDao.unlockSkuStock(skuId, wareId, num);
        //更新工作单的状态为已经解锁
        WareOrderTaskDetailEntity taskDetailEntity = new WareOrderTaskDetailEntity();
        taskDetailEntity.setId(detailId);
        taskDetailEntity.setLockStatus(2);
        wareOrderTaskDetailService.updateById(taskDetailEntity);
    }

    //解锁库存(主动)：防止订单服务卡顿，库存延时队列优先到期，导致锁定的库存永远无法解锁
    @Override
    @Transactional
    public void unlockStock(OrderTo order) {
        WareOrderTaskEntity taskEntity = wareOrderTaskService.getOrderTaskByOrderSn(order.getOrderSn());
        //查询最新的库存状态，防止重复解锁库存
        //极端情况下，主动解锁与被动解锁同时到期，可能会出现并发问题，导致重复解锁
        List<WareOrderTaskDetailEntity> list = wareOrderTaskDetailService.list(new QueryWrapper<WareOrderTaskDetailEntity>()
                .eq("task_id", taskEntity.getId()).eq("lock_status", 1));
        for(WareOrderTaskDetailEntity taskDetailEntity : list){
            orderUnlockStock(taskDetailEntity.getSkuId(), taskDetailEntity.getWareId(), taskDetailEntity.getSkuNum(), taskDetailEntity.getId());
        }
    }
}














