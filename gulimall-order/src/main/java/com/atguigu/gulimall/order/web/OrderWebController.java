package com.atguigu.gulimall.order.web;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.exception.OrderException;
import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.vo.OrderConfirmVo;
import com.atguigu.gulimall.order.vo.OrderSubmitVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.concurrent.ExecutionException;

@Controller
public class OrderWebController {
    @Autowired
    private OrderService orderService;

    //跳转结算页
    @GetMapping("/toTrade")
    public String toTrade(Model model) throws ExecutionException, InterruptedException {
        OrderConfirmVo orderConfirmVo = orderService.confirmOrder();
        model.addAttribute("orderConfirmData", orderConfirmVo);
        return "confirm";
    }
    //提交订单
    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo vo, Model model, RedirectAttributes redirectAttributes) {
        try{
            OrderEntity order = orderService.submitOrder(vo);
            //下单成功，前往支付页
            model.addAttribute("order", order);
            return "pay";
        }catch(Exception e){
            //下单失败，返回结算页，重新确认订单信息
            if(e instanceof OrderException){
                redirectAttributes.addFlashAttribute("msg", e.getMessage());
            }
            return "redirect:http://order.gulimall.com/toTrade";
        }
    }
}
