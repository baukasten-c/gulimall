package com.atguigu.gulimall.order.web;

import com.atguigu.gulimall.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class PayWebController {
    @Autowired
    private OrderService orderService;

    //支付宝支付
    @GetMapping(value = "/payOrder", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String payOrder(@RequestParam("orderSn") String orderSn){
        String pay = orderService.getOrderPay(orderSn);
        return pay;
    }
}
