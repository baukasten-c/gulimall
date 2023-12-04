package com.atguigu.gulimall.order.config;

import com.atguigu.common.constant.OrderConstant;
import com.atguigu.common.constant.WareConstant;
import org.springframework.amqp.core.*;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class MyRabbitMQConfig {
    @Bean
    public Binding orderCreateBinding(@Qualifier("orderDelayQueue") Queue queue, @Qualifier("orderEventExchange") Exchange exchange){
        //目的地(队列名或交换机名)、目的地类型(队列或交换机)、绑定交换机、绑定路由、可选参数
//        return new Binding("order.delay.queue", Binding.DestinationType.QUEUE,
//                "order-event-exchange", "order.create.order", null);
        return BindingBuilder.bind(queue).to(exchange).with(OrderConstant.ORDER_DELAY_QUEUE_KEY).noargs();
    }

    //普通交换机+死信交换机
    @Bean
    public Exchange orderEventExchange(){
//        return new TopicExchange("order-event-exchange", true, false);
        return ExchangeBuilder.topicExchange(OrderConstant.ORDER_EXCHANGE).durable(true).build();
    }

    //延迟队列，产生死信
    @Bean
    public Queue orderDelayQueue(){
//        HashMap<String, Object> arguments = new HashMap<>();
//        //死信交换机
//        arguments.put("x-dead-letter-exchange", "order-event-exchange");
//        //死信路由
//        arguments.put("x-dead-letter-routing-key", "order.release.order");
//        //消息过期时间：1分钟
//        arguments.put("x-message-ttl", 60000);
//        //队列名称、是否持久(服务器重启后保留)、是否排他(声明队列的连接可以使用)、是否自动删除、可选参数(用于指定队列的其他属性和行为)
//        return new Queue("order.delay.queue", true, false, false, arguments);
        return QueueBuilder.durable(OrderConstant.ORDER_DELAY_QUEUE)
                .withArgument("x-dead-letter-exchange", OrderConstant.ORDER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", OrderConstant.ORDER_RELEASE_QUEUE_KEY)
                .withArgument("x-message-ttl", TimeUnit.MINUTES.toMillis(1)).build();
    }

    @Bean
    public Binding orderReleaseBinding(@Qualifier("orderReleaseQueue") Queue queue, @Qualifier("orderEventExchange") Exchange exchange){
//        return new Binding("order.release.order.queue", Binding.DestinationType.QUEUE,
//                "order-event-exchange", "order.release.order", null);
        return BindingBuilder.bind(queue).to(exchange).with(OrderConstant.ORDER_RELEASE_QUEUE_KEY).noargs();
    }

    //死信队列，接收死信
    @Bean
    public Queue orderReleaseQueue(){
//        return new Queue("order.release.order.queue", true, false, false);
        return QueueBuilder.durable(OrderConstant.ORDER_RELEASE_QUEUE).build();
    }

    //绑定关闭订单与解锁库存
    @Bean
    public Binding orderReleaseOtherBinding() {
        return new Binding(WareConstant.STOCK_RELEASE_QUEUE, Binding.DestinationType.QUEUE, WareConstant.STOCK_EXCHANGE,
                OrderConstant.ORDER_RELEASE_QUEUE_KEY + ".#", null);
    }
}
