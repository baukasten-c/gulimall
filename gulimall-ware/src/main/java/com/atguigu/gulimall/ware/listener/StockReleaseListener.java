package com.atguigu.gulimall.ware.listener;

import com.atguigu.common.constant.WareConstant;
import com.atguigu.common.to.OrderTo;
import com.atguigu.gulimall.ware.service.WareSkuService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RabbitListener(queues = WareConstant.STOCK_RELEASE_QUEUE, ackMode = "MANUAL")
public class StockReleaseListener {
    @Autowired
    private WareSkuService wareSkuService;

    @RabbitHandler
    public void handleStockLockedRelease(Long detailId, Message message, Channel channel) throws IOException {
        try{
            //解锁库存
            wareSkuService.unlockStock(detailId);
            //开启手动确认消息，避免出现问题导致信息丢失
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false); //multiple=false：非批量模式
        }catch(Exception e){
            //解锁失败，将消息重新放回队列
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true); //requeue=true：在服务器重新入队
        }
    }

    @RabbitHandler
    public void handleOrderCloseRelease(OrderTo order, Message message, Channel channel) throws IOException {
        try{
            //解锁库存
            wareSkuService.unlockStock(order);
            //开启手动确认消息，避免出现问题导致信息丢失
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false); //multiple=false：非批量模式
        }catch(Exception e){
            //解锁失败，将消息重新放回队列
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true); //requeue=true：在服务器重新入队
        }
    }
}
