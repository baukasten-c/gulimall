package com.atguigu.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderSubmitVo {
    /**
     * 地址id
     */
    private Long addrId;
    /**
     * 支付方式
     */
    private Integer payType;
    /**
     * 防重令牌
     */
    private String orderToken;
    /**
     * 应付价格
     */
    private BigDecimal payPrice;
    /**
     * 运费
     */
    private BigDecimal farePrice;
    /**
     * 订单备注
     */
    private String note;

    //用户相关信息，在session中取出即可
    //购买商品需要重新获得，以保持最新数据
}
