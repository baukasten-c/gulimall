package com.atguigu.gulimall.cart.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class CopyOnWriteArrayListMixin {
    @JsonCreator
    CopyOnWriteArrayListMixin(@JsonProperty("array") Object[] array) {}
}
