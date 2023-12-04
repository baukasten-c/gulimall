package com.atguigu.gulimall.cart.vo;

import java.math.BigDecimal;
import java.util.List;

public class CartVo { //购物车
    /**
     * 购物车子项信息
     */
    private List<CartItemVo> items;
    /**
     * 商品数量
     */
    private Integer countNum;
    /**
     * 商品类型数量
     */
    private Integer countType;
    /**
     * 商品总价
     */
    private BigDecimal totalAmount;
    /**
     * 减免价格
     */
    private BigDecimal reduce = new BigDecimal("0.00");;
    public List<CartItemVo> getItems() {
        return items;
    }
    public void setItems(List<CartItemVo> items) {
        this.items = items;
    }
    public Integer getCountNum() {
        return items.stream().mapToInt(CartItemVo::getCount).sum();
    }
    public void setCountNum(Integer countNum) {
        this.countNum = countNum;
    }
    public Integer getCountType() {
        return items.size();
    }
    public void setCountType(Integer countType) {
        this.countType = countType;
    }
    public BigDecimal getTotalAmount() {
        BigDecimal amount = items.stream()
                //只计算被选中的商品
                .filter(CartItemVo::getCheck)
                .map(CartItemVo::getTotalPrice)
                //从0开始累加
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // 计算优惠后的价格
        return amount.subtract(reduce);
    }
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
    public BigDecimal getReduce() {
        return reduce;
    }
    public void setReduce(BigDecimal reduce) {
        this.reduce = reduce;
    }
}
