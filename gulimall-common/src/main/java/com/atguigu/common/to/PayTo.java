package com.atguigu.common.to;

import com.alibaba.fastjson.JSONArray;
import lombok.Data;

@Data
public class PayTo {
    /**
     * 商户订单号 必选
     */
    private String outTradeNo;

    /**
     * 订单总金额 必选
     */
    private String totalAmount;

    /**
     * 订单标题 必选
     */
    private String subject;

    /**
     * 订单包含的商品列表信息 可选
     */
    private JSONArray goodsDetail;
}