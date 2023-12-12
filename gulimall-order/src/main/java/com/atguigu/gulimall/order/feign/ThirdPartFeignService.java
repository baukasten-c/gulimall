package com.atguigu.gulimall.order.feign;

import com.atguigu.common.to.PayTo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("gulimall-third-party")
public interface ThirdPartFeignService {
    @PostMapping("/thirdparty/pay/alipay")
    String pay(@RequestBody PayTo to);
}
