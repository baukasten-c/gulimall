package com.atguigu.gulimall.thirdparty.controller;

import com.atguigu.common.utils.R;
import com.atguigu.gulimall.thirdparty.component.SmsComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("sms")
public class SmsController {
    @Autowired
    private SmsComponent smsComponent;

    //用于其他服务进行调用
    @GetMapping("/sendCode")
    public R sendCode(@RequestParam("mobile") String mobile, @RequestParam("code") String code){
        smsComponent.sendSmsCode(mobile, code);
        return R.ok();
    }
}
