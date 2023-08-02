package com.atguigu.gulimall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration //相当于把该类作为spring的xml配置文件中的<beans>
public class GulimallCorsConfiguration {
    @Bean
    public CorsWebFilter corsWebFilter(){
        //创建基于URL的CORS配置源
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        //创建一个CORS配置对象
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        //允许所有请求头
        corsConfiguration.addAllowedHeader("*");
        //允许所有请求方法（GET、POST、PUT等）
        corsConfiguration.addAllowedMethod("*");
        //允许所有来源（域名），即允许所有跨域请求
        corsConfiguration.addAllowedOriginPattern("*");
        //允许发送凭据信息，例如Cookie。如果需要在跨域请求中发送和接收Cookie，此选项必须为true
        corsConfiguration.setAllowCredentials(true);
        //将CORS配置应用到所有的URL路径上（"/**"表示匹配所有路径）
        source.registerCorsConfiguration("/**", corsConfiguration);
        //创建CorsWebFilter并使用上述配置源进行过滤
        return new CorsWebFilter(source);
    }
}
