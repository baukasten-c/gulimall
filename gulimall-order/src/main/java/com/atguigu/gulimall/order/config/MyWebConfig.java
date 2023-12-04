package com.atguigu.gulimall.order.config;

import com.atguigu.gulimall.order.interceptor.OrderInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MyWebConfig implements WebMvcConfigurer {
    @Autowired
    private OrderInterceptor orderInterceptor;

    //配置拦截器
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //拦截所有请求
        registry.addInterceptor(orderInterceptor).addPathPatterns("/**");
    }
}
