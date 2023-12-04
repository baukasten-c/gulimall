package com.atguigu.gulimall.ware.config;

import com.atguigu.common.constant.WareConstant;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class MyRabbitMQConfig {
    @Bean
    public MessageConverter messageConverter() {
        //将rabbitmq发送的消息转为json格式
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Binding stockCreateBinding(@Qualifier("stockDelayQueue") Queue queue, @Qualifier("stockEventExchange") Exchange exchange){
        //目的地(队列名或交换机名)、绑定交换机、绑定路由、可选参数
        return BindingBuilder.bind(queue).to(exchange).with(WareConstant.STOCK_DELAY_QUEUE_KEY).noargs();
    }

    //普通交换机+死信交换机
    @Bean
    public Exchange stockEventExchange(){
        return ExchangeBuilder.topicExchange(WareConstant.STOCK_EXCHANGE).durable(true).build();
    }

    //延迟队列，产生死信
    @Bean
    public Queue stockDelayQueue(){
        return QueueBuilder.durable(WareConstant.STOCK_DELAY_QUEUE) //队列名称，持久(服务器重启后保留)
                .withArgument("x-dead-letter-exchange", WareConstant.STOCK_EXCHANGE) //死信交换机
                .withArgument("x-dead-letter-routing-key", WareConstant.STOCK_RELEASE_QUEUE_KEY) //死信路由
                .withArgument("x-message-ttl", TimeUnit.MINUTES.toMillis(2)).build(); //消息过期时间：2分钟
    }

    @Bean
    public Binding stockReleaseBinding(@Qualifier("stockReleaseQueue") Queue queue, @Qualifier("stockEventExchange") Exchange exchange){
        return BindingBuilder.bind(queue).to(exchange).with(WareConstant.STOCK_RELEASE_QUEUE_KEY + ".#").noargs();
    }

    //死信队列，接收死信
    @Bean
    public Queue stockReleaseQueue(){
        return QueueBuilder.durable(WareConstant.STOCK_RELEASE_QUEUE).build();
    }
}
