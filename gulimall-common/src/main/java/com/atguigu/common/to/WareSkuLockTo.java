package com.atguigu.common.to;


import lombok.Data;

import java.util.List;

@Data
public class WareSkuLockTo {
    private String orderSn;
    /**
     * 所有需要锁定的订单项信息
     */
    private List<OrderItemTo> locks;
}
