package com.atguigu.gulimall.order.web;

import com.atguigu.gulimall.order.entity.OrderEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.UUID;

@Controller
public class HelloController {
    //测试各个页面是否可以正常显示
    @GetMapping(value = "/{page}.html")
    public String listPage(@PathVariable("page") String page) {
        return page;
    }
}
