package com.atguigu.gulimall.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MyWebConfig implements WebMvcConfigurer {
    //视图映射
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        //不做任何处理直接进行跳转
        registry.addViewController("/reg.html").setViewName("reg");
    }
}
