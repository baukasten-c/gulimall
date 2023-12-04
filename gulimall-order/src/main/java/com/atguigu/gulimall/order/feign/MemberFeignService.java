package com.atguigu.gulimall.order.feign;

import com.atguigu.gulimall.order.vo.MemberAddressVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient("gulimall-member")
public interface MemberFeignService {
    @GetMapping("/member/memberreceiveaddress/address/{memberId}")
    List<MemberAddressVo> getAddress(@PathVariable("memberId") Long memberId);
    @GetMapping("/member/memberreceiveaddress/addressinfo/{id}")
    MemberAddressVo getAddressById(@PathVariable("id") Long id);
}
