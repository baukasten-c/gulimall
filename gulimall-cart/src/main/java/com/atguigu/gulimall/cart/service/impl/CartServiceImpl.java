package com.atguigu.gulimall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.constant.CartConstant;
import com.atguigu.common.to.UserInfoTo;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.cart.feign.ProductFeignService;
import com.atguigu.gulimall.cart.interceptor.CartInterceptor;
import com.atguigu.gulimall.cart.service.CartService;
import com.atguigu.gulimall.cart.vo.CartItemVo;
import com.atguigu.gulimall.cart.vo.CartVo;
import com.atguigu.gulimall.cart.vo.SkuInfoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ProductFeignService productFeignService;
    @Autowired
    private ThreadPoolExecutor executor;

    //获取购物车
    private BoundHashOperations<String, Object, Object> getCartOps(){
        //获取当前用户信息
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        String cartKey = CartConstant.CART_PREFIX + (userInfoTo.getUserId() != null ? userInfoTo.getUserId() : userInfoTo.getUserKey());
        //绑定key操作redis
        BoundHashOperations<String, Object, Object> cartOps = redisTemplate.boundHashOps(cartKey);
        return cartOps;
    }

    //将商品加入购物车
    @Override
    public void addCartItem(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        //获取购物车
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        //判断是否为新选商品
        String cartJson = (String) cartOps.get(skuId.toString());
        if(StringUtils.hasLength(cartJson)){
            //购物车中有该商品，修改数量
            CartItemVo cartItemVo = JSON.parseObject(cartJson, CartItemVo.class);
            cartItemVo.setCount(cartItemVo.getCount() + num);
            //修改redis中的商品信息
            cartJson = JSON.toJSONString(cartItemVo);
            cartOps.put(skuId.toString(), cartJson);
        }else{
            //购物车中无该商品，新增
            CartItemVo cartItemVo = new CartItemVo();
            //获取添加商品信息
            CompletableFuture<Void> skuInfoFuture = CompletableFuture.runAsync(() -> {
                R r = productFeignService.info(skuId);
                SkuInfoVo skuInfo = r.getData("skuInfo", new TypeReference<SkuInfoVo>(){});
                cartItemVo.setSkuId(skuId);
                cartItemVo.setCheck(true);
                cartItemVo.setTitle(skuInfo.getSkuTitle());
                cartItemVo.setImage(skuInfo.getSkuDefaultImg());
                cartItemVo.setPrice(skuInfo.getPrice());
                cartItemVo.setCount(num);
            }, executor);
            //获取添加商品销售属性值列表
            CompletableFuture<Void> skuAttrValuesFuture = CompletableFuture.runAsync(() -> {
                List<String> attrValuesList = productFeignService.getSkuSaleAttrValuesAsList(skuId);
                cartItemVo.setSkuAttrValues(attrValuesList);
            }, executor);
            //等待异步任务完成
            CompletableFuture.allOf(skuInfoFuture, skuAttrValuesFuture).get();
            //商品信息存入redis
            //JSON.toJSONString()会调用所有属性的get方法,totalPrice此时自动更新
            cartJson = JSON.toJSONString(cartItemVo);
            cartOps.put(skuId.toString(), cartJson);
        }
    }

    //获取购物车中商品信息
    @Override
    public CartItemVo getCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String cartJson = (String) cartOps.get(skuId.toString());
        CartItemVo cartItemVo = JSON.parseObject(cartJson, CartItemVo.class);
        return cartItemVo;
    }

    //获取购物车中所有商品信息
    public List<CartItemVo> getCartItems(String cartKey) {
        BoundHashOperations<String, Object, Object> cartOps = redisTemplate.boundHashOps(cartKey);
        List<Object> cartItems = cartOps.values();
        if(!CollectionUtils.isEmpty(cartItems)){
            List<CartItemVo> cartItemVos = cartItems.stream().map(item -> {
                String cartJson = (String) item;
                CartItemVo cartItemVo = JSON.parseObject(cartJson, CartItemVo.class);
                return cartItemVo;
            }).collect(Collectors.toList());
            return cartItemVos;
        }
        return null;
    }

    //获取购物车
    @Override
    public CartVo getCart() throws ExecutionException, InterruptedException {
        CartVo cartVo = new CartVo();
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        //获取临时用户购物车
        String temptCartKey = CartConstant.CART_PREFIX + userInfoTo.getUserKey();
        List<CartItemVo> tempCartItems = getCartItems(temptCartKey);
        if(userInfoTo.getUserId() != null){ //登录
            //获取用户购物车
            String cartKey = CartConstant.CART_PREFIX + userInfoTo.getUserId();
            List<CartItemVo> cartItems = getCartItems(cartKey);
            if(!CollectionUtils.isEmpty(tempCartItems)){
                //合并临时用户购物车
                for(CartItemVo item : tempCartItems){
                    addCartItem(item.getSkuId(), item.getCount());
                }
                //清除临时用户购物车中商品
                clearCart(temptCartKey);
            }
            cartVo.setItems(cartItems);
        }else{ //未登录
            cartVo.setItems(tempCartItems);
        }
        return cartVo;
    }

    //清除临时用户购物车中商品
    public void clearCart(String temptCartKey) {
        redisTemplate.delete(temptCartKey);
    }

    //勾选购物车中商品
    @Override
    public void checkItem(Long skuId, Integer check) {
        CartItemVo cartItemVo = getCartItem(skuId);
        cartItemVo.setCheck(check == 1);
        //修改后的商品信息存入redis
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String cartJson = JSON.toJSONString(cartItemVo);
        cartOps.put(skuId.toString(), cartJson);
    }

    //修改购物车中商品数量
    @Override
    public void countItem(Long skuId, Integer num) {
        CartItemVo cartItemVo = getCartItem(skuId);
        cartItemVo.setCount(num);
        //修改后的商品信息存入redis
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String cartJson = JSON.toJSONString(cartItemVo);
        cartOps.put(skuId.toString(), cartJson);
    }

    //删除购物车中商品
    @Override
    public void deleteItem(Integer skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.delete(skuId.toString());
    }

    //获取当前用户购物车中所有购物项
    @Override
    public List<CartItemVo> getUserCartItems() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        String cartKey = CartConstant.CART_PREFIX + userInfoTo.getUserId();
        List<CartItemVo> cartItems = getCartItems(cartKey).stream().filter(CartItemVo::getCheck).collect(Collectors.toList());
        //一次性获取所有sku的最新价格
        List<Long> skuIds = cartItems.stream().map(CartItemVo::getSkuId).collect(Collectors.toList());
        Map<Long, Map<String, Object>> pricesValue = productFeignService.getPrices(skuIds);
        //一次性获取所有spu的重量
        List<Long> spuIds = pricesValue.values().stream().map(m -> Long.parseLong(m.get("spuId").toString())).distinct().collect(Collectors.toList());
        Map<Long, Map<String, BigDecimal>> weightsValue = productFeignService.getWeights(spuIds);
        cartItems = cartItems.stream().map(item -> {
            //更新商品价格为最新价格
            BigDecimal price = new BigDecimal(pricesValue.get(item.getSkuId()).get("price").toString());
            item.setPrice(price);
            //获取商品重量
            Long spuId = Long.parseLong(pricesValue.get(item.getSkuId()).get("spuId").toString());
            BigDecimal weight = weightsValue.get(spuId).get("weight");
            item.setWeight(weight);
            return item;
        }).collect(Collectors.toList());
        return cartItems;
    }
}
