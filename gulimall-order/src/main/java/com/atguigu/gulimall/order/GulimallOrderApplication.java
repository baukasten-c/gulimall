package com.atguigu.gulimall.order;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@SpringBootApplication
@EnableDiscoveryClient
@EnableRabbit
@EnableRedisHttpSession
@EnableFeignClients
//@EnableAspectJAutoProxy(exposeProxy = true) //开启aspectj动态代理，没有接口也可以代理，exposeProxy=true表示对外暴露代理对象
public class GulimallOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallOrderApplication.class, args);
    }

}
