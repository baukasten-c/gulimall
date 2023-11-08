package com.atguigu.gulimall.auth.feign;

import com.atguigu.common.to.SocialUserTo;
import com.atguigu.common.to.UserLoginTo;
import com.atguigu.common.to.UserRegistTo;
import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("gulimall-member")
public interface MemberFeignService {
    @PostMapping("/member/member/register")
    R register(@RequestBody UserRegistTo to);
    @PostMapping("/member/member/login")
    R login(@RequestBody UserLoginTo to);
    @PostMapping("/member/member/oauth2/login")
    R login(@RequestBody SocialUserTo to);
}
