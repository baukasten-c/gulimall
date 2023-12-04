package com.atguigu.common.constant;

public class OrderConstant {
    public static final String USER_ORDER_TOKEN_PREFIX = "order:token:";
    public static final String ORDER_EXCHANGE = "order-event-exchange";
    public static final String ORDER_DELAY_QUEUE = "order.delay.queue";
    public static final String ORDER_RELEASE_QUEUE = "order.release.order.queue";
    public static final String ORDER_DELAY_QUEUE_KEY = "order.create.order";
    public static final String ORDER_RELEASE_QUEUE_KEY = "order.release.order";
    public enum OrderStatusEnum {
        CREATE_NEW(0,"待付款"),
        PAYED(1,"已付款"),
        SENDED(2,"已发货"),
        RECIEVED(3,"已完成"),
        CANCLED(4,"已取消"),
        SERVICING(5,"售后中"),
        SERVICED(6,"售后完成");
        private Integer code;
        private String msg;
        OrderStatusEnum(Integer code, String msg) {
            this.code = code;
            this.msg = msg;
        }
        public Integer getCode() {
            return code;
        }
        public String getMsg() {
            return msg;
        }
    }
}
