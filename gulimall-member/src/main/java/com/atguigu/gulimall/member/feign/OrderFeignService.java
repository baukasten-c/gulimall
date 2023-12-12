package com.atguigu.gulimall.member.feign;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient("gulimall-order")
public interface OrderFeignService {
    @GetMapping("/order/order/listWithItems")
    R listWithItems(@RequestParam Map<String, Object> params);
}
