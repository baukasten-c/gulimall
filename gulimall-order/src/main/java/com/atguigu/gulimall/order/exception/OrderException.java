package com.atguigu.gulimall.order.exception;

import lombok.Getter;
import lombok.Setter;

public class OrderException extends RuntimeException {
    public OrderException(String msg) {
        super(msg);
    }
}
