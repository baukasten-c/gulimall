package com.atguigu.gulimall.cart.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FlashMapMixin {
    @JsonCreator
    FlashMapMixin(@JsonProperty("targetRequestPath") String targetRequestPath) {}
}
