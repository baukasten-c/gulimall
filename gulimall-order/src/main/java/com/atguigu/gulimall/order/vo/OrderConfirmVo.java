package com.atguigu.gulimall.order.vo;


import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class OrderConfirmVo {
    //地址列表信息
    @Getter
    @Setter
    private List<MemberAddressVo> address;
    //购物项信息
    @Getter
    @Setter
    private List<OrderItemVo> items;
    //会员积分信息
    @Getter
    @Setter
    private Integer integration;
    //订单总额
    @Setter
    private BigDecimal totalPrice;
    //购物项总数量
    @Setter
    private Integer count;
    //购物项库存
    @Getter
    @Setter
    private Map<Long, Boolean> stocks;
    //商品总重量
    @Setter
    private BigDecimal weights;
    //唯一令牌(防止重复付款)
    @Getter
    @Setter
    private String orderToken;

    public BigDecimal getTotalPrice(){
        BigDecimal total = items.stream()
                //商品有库存
                .filter(item -> stocks.get(item.getSkuId()))
                //当前商品的总价格
                .map(item -> item.getPrice().multiply(new BigDecimal(item.getCount())))
                //从0开始累加
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total;
    }
    public Integer getCount(){
        return items.stream().filter(item -> stocks.get(item.getSkuId())).map(OrderItemVo::getCount).reduce(0, Integer::sum);
    }
    public BigDecimal getWeights(){
        BigDecimal weights = items.stream()
                .filter(item -> stocks.get(item.getSkuId()))
                .map(item -> item.getWeight())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return weights;
    }
}
