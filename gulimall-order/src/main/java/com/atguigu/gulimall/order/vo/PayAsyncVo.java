package com.atguigu.gulimall.order.vo;

import lombok.Data;
import lombok.ToString;

@Data
public class PayAsyncVo {
    private String gmt_create; //交易创建时间
    private String charset; //编码格式
    private String gmt_payment; //交易付款时间
    private String notify_time; //通知的发送时间
    private String subject; //订单标题/商品标题/交易标题/订单关键字等
    private String sign; //签名
    private String buyer_id; //支付者的id
    private String body; //订单的信息
    private String invoice_amount; //支付金额
    private String version; //调用的接口版本
    private String notify_id; //通知id
    private String fund_bill_list; //支付金额信息
    private String notify_type; //通知类型； trade_status_sync
    private String out_trade_no; //订单号
    private String total_amount; //支付的总额
    private String trade_status; //交易状态  TRADE_SUCCESS
    private String trade_no; //流水号
    private String auth_app_id; //授权方的APPID
    private String receipt_amount; //商家收到的款
    private String point_amount; //使用集分宝支付金额
    private String buyer_pay_amount; //最终支付的金额
    private String app_id; //应用id
    private String sign_type; //签名类型
    private String seller_id; //商家的id

}
