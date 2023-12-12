package com.atguigu.gulimall.member.config;

import com.atguigu.gulimall.member.interceptor.MemberInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MyWebConfig implements WebMvcConfigurer {
    @Autowired
    private MemberInterceptor memberInterceptor;

    //配置拦截器
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //拦截需要登录的请求
        registry.addInterceptor(memberInterceptor).addPathPatterns("/memberOrder.html");
    }
}
