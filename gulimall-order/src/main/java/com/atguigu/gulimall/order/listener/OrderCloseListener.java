package com.atguigu.gulimall.order.listener;

import com.atguigu.common.constant.OrderConstant;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RabbitListener(queues = OrderConstant.ORDER_RELEASE_QUEUE)
public class OrderCloseListener {
    @Autowired
    private OrderService orderService;

    @RabbitHandler
    public void handleOrderRelease(OrderEntity order, Message message, Channel channel) throws IOException {
        try{
            //定时关闭订单
            orderService.closeOrder(order);
            //开启手动确认消息，避免出现问题导致信息丢失
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false); //multiple=false：非批量模式
        }catch(Exception e){
            //关闭失败,将消息重新放回队列
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true); //requeue=true：在服务器重新入队
        }
    }
}
