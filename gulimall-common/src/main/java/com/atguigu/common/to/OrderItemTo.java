package com.atguigu.common.to;

import lombok.Data;

@Data
public class OrderItemTo {
    private Long skuId;
    private String skuName;
    private Integer count;
}
