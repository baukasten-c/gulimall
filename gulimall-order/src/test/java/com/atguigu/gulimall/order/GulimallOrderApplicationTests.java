package com.atguigu.gulimall.order;

import com.atguigu.common.constant.OrderConstant;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderReturnReasonEntity;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@SpringBootTest
@Slf4j
@EnableRabbit
//@RabbitListener(queues = {"hello-java-queue"}) //queues：需要监听的队列
class GulimallOrderApplicationTests {
    @Autowired
    private AmqpAdmin amqpAdmin;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    //测试延时队列
    @Test
    public void createOrderTest() {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(UUID.randomUUID().toString());
        orderEntity.setCreateTime(LocalDateTime.now());
        rabbitTemplate.convertAndSend(OrderConstant.ORDER_EXCHANGE, OrderConstant.ORDER_DELAY_QUEUE_KEY, orderEntity);
    }

    //----------------------------------------熟悉RabbitMQ----------------------------------------
    @Test
    public void createExchange() {
        DirectExchange directExchange = new DirectExchange("hello-java-exchange", true, false);
        amqpAdmin.declareExchange(directExchange);
        log.info("Exchange[{}]创建成功", "hello-java-exchange");
    }

    @Test
    public void createQueue() {
        Queue queue = new Queue("hello-java-queue", true, false, false);
        amqpAdmin.declareQueue(queue);
        log.info("Queue[{}]创建成功", "hello-java-queue");
    }

    @Test
    public void createBinding() {
        Binding binding = new Binding("hello-java-queue",
                Binding.DestinationType.QUEUE,
                "hello-java-exchange",
                "hello.java",
                null);
        amqpAdmin.declareBinding(binding);
        log.info("Binding[{}]创建成功", "hello-java-binding");
    }

    @Test
    public void sendMessageString() {
        String msg = "Hello World";
        rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java", msg);
        log.info("消息[{}]发送完成", msg);
    }

    @Test
    public void sendMessageObject() {
        for(int i = 0; i < 6; i++){
            if(i % 2 == 0){
                //发送消息，如果发送的消息是个对象，会使用序列化机制将对象写出去，因此对象必须实现Serializable接口
                OrderReturnReasonEntity reasonEntity = new OrderReturnReasonEntity();
                reasonEntity.setId(Long.parseLong(i + ""));
                reasonEntity.setCreateTime(new Date());
                rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java", reasonEntity, new CorrelationData(i + ""));
            }else{
                OrderEntity orderEntity = new OrderEntity();
                orderEntity.setId(Long.parseLong(i + ""));
                rabbitTemplate.convertAndSend("hello-java-exchange", "hello1.java", orderEntity, new CorrelationData(i + ""));
            }
            log.info("消息[{}]发送完成", i);
        }
    }

    @RabbitListener(queues = {"hello-java-queue"})
    //参数：Message：原生消息信息(头+体)；T<发送的消息类型> content：消息内容；Channel：当前传输数据的通道
    public void revieveMessage(Message msg, OrderReturnReasonEntity content, Channel channel) {
        //消息头属性信息
        MessageProperties messageProperties = msg.getMessageProperties();
        //消息主体内容
        byte[] body = msg.getBody();
        System.out.println("消息[" + content + "]接收完成，消息头[" + messageProperties + "]，消息体[" + new String(body) + "]");
        //签收信息
        long deliveryTag = messageProperties.getDeliveryTag();
        try{
            if(deliveryTag % 2 == 0){
                //签收
                channel.basicAck(deliveryTag, false); //multiple=false：非批量模式
                System.out.println("签收了消息：" + deliveryTag);
            }else{
                //退回
                //requeue=false：丢弃，requeue=true：发会服务器，在服务器重新入队
                channel.basicNack(deliveryTag, false, false);
                System.out.println("退回了消息：" + deliveryTag);
            }
        }catch (IOException e){ //网络中断
            throw new RuntimeException(e);
        }
    }

//    @RabbitHandler
    public void revieveMessage1(OrderReturnReasonEntity content) {
        System.out.println("消息[" + content + "]接收完成");
    }

//    @RabbitHandler
    public void revieveMessage2(OrderEntity content) {
        System.out.println("消息[" + content + "]接收完成");
    }
}
