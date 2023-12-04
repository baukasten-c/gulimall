package com.atguigu.gulimall.ware.exception;

import lombok.Getter;
import lombok.Setter;

public class NoStockException extends RuntimeException {
    @Getter
    @Setter
    private Long skuId;

    public NoStockException(Long skuId) {
        super("商品"+ skuId + "库存不足！");
    }

    public NoStockException(String msg) {
        super(msg);
    }
}
