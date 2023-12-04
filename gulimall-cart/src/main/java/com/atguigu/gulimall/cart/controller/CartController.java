package com.atguigu.gulimall.cart.controller;

import com.atguigu.gulimall.cart.service.CartService;
import com.atguigu.gulimall.cart.vo.CartItemVo;
import com.atguigu.gulimall.cart.vo.CartVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Controller
public class CartController {
    @Autowired
    private CartService cartService;

    //获取购物车
    @GetMapping("/cart.html")
    public String cartListPage(Model model) throws ExecutionException, InterruptedException {
        CartVo cartVo = cartService.getCart();
        model.addAttribute("cart", cartVo);
        return "cartList";
    }

    //将商品加入购物车
    @GetMapping("/addCartItem")
    public String addCartItem(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num,
                              RedirectAttributes redirectAttributes) throws ExecutionException, InterruptedException {
        cartService.addCartItem(skuId, num);
        //addFlashAttribute()：隐藏参数，数据放在session里面，可以在页面取出，但只能取一次
        //addAttribute()：数据放在url后面，可重复取数据
        redirectAttributes.addAttribute("skuId", skuId);
        //使用重定向，避免刷新后重复提交
        return "redirect:http://cart.gulimall.com/addCartItemSuccess.html";
    }

    //跳转到添加购物车成功页面
    @GetMapping("/addCartItemSuccess.html")
    public String addToCartSuccessPage(@RequestParam("skuId") Long skuId, Model model) {
        //获取购物车中商品信息
        CartItemVo cartItemVo = cartService.getCartItem(skuId);
        model.addAttribute("item", cartItemVo);
        return "success";
    }

    //勾选购物车中商品
    @GetMapping("/checkItem")
    public String checkItem(@RequestParam("skuId") Long skuId, @RequestParam("check") Integer check) {
        cartService.checkItem(skuId, check);
        return "redirect:http://cart.gulimall.com/cart.html";
    }

    //修改购物车中商品数量
    @GetMapping("/countItem")
    public String countItem(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num) {
        cartService.countItem(skuId, num);
        return "redirect:http://cart.gulimall.com/cart.html";
    }

    //删除购物车中商品
    @GetMapping("/deleteItem")
    public String deleteItem(@RequestParam("skuId") Integer skuId) {
        cartService.deleteItem(skuId);
        return "redirect:http://cart.gulimall.com/cart.html";
    }

    //获取当前用户购物车中所有购物项
    @GetMapping("/currentUserCartItems")
    @ResponseBody
    public List<CartItemVo> getUserCartItems(){
        return cartService.getUserCartItems();
    }
}
