package com.atguigu.gulimall.order.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class CopyOnWriteArrayListMixin {
    @JsonCreator
    CopyOnWriteArrayListMixin(@JsonProperty("array") Object[] array) {}
}
