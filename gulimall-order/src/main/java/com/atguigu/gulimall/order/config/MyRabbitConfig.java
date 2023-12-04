package com.atguigu.gulimall.order.config;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyRabbitConfig {
    private RabbitTemplate rabbitTemplate;

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        this.rabbitTemplate = rabbitTemplate;
        rabbitTemplate.setMessageConverter(messageConverter());
        //mandatory(交换器无法根据自身类型和路由键找到一个符合条件的队列时的处理方式)：true-将消息返回给生产者，false-把消息直接丢弃
        rabbitTemplate.setMandatory(true);
        initRabbitTemplate();
        return rabbitTemplate;
    }

    @Bean
    public MessageConverter messageConverter() {
        //将rabbitmq发送的消息转为json格式
        return new Jackson2JsonMessageConverter();
    }

    //定制RabbitTemplate：设置确认回调
    public void initRabbitTemplate() {
        //Broker代理收到消息就会回调
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            @Override
            public void confirm(CorrelationData correlationData, boolean ack, String cause) {
                //correlationData：当前消息的唯一关联数据(通过消息的唯一id)
                //ack：消息是否成功收到(只要消息抵达Broker，ack就为true)
                //cause：接收消息失败的原因
                System.out.println("confirm：correlationData[" + correlationData + "]=>ack:[" + ack +  "]=>cause:[" + cause + "]");
            }
        });
        //指定队列未收到消息就会回调
        rabbitTemplate.setReturnsCallback(new RabbitTemplate.ReturnsCallback() {
            @Override
            public void returnedMessage(ReturnedMessage returnedMessage) {
                //投递失败的消息的详细信息
                Message message = returnedMessage.getMessage();
                //回复的状态码
                int replyCode = returnedMessage.getReplyCode();
                //回复的文本内容
                String replyText = returnedMessage.getReplyText();
                //消息发给的交换机
                String exchange = returnedMessage.getExchange();
                //消息使用的路邮键
                String routingKey = returnedMessage.getRoutingKey();
                System.out.println("Fail Message[" + message + "]=>replyCode[" + replyCode + "]" +
                        "=>replyText[" + replyText + "]=>exchange[" + exchange + "]=>routingKey[" + routingKey + "]");
            }
        });
    }
}
