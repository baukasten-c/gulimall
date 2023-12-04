package com.atguigu.gulimall.cart;

import com.atguigu.gulimall.cart.service.CartService;
import com.atguigu.gulimall.cart.vo.CartItemVo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

@SpringBootTest
class GulimallCartApplicationTests {
    @Autowired
    private CartService cartService;

    @Test
    void testGetUserCartItems() {
        long stime = System.currentTimeMillis();
        List<CartItemVo> userCartItems = cartService.getUserCartItems();
        System.out.println(Arrays.toString(userCartItems.toArray()));
        long etime = System.currentTimeMillis();
        System.out.printf("执行时长：%d 毫秒.", (etime - stime));
    }

}
