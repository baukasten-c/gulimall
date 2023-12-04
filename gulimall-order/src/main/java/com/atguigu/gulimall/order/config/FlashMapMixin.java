package com.atguigu.gulimall.order.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FlashMapMixin {
    @JsonCreator
    FlashMapMixin(@JsonProperty("targetRequestPath") String targetRequestPath) {}
}
