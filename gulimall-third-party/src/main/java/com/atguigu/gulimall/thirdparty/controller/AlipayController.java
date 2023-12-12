package com.atguigu.gulimall.thirdparty.controller;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.atguigu.common.to.PayTo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("thirdparty/pay")
public class AlipayController {
    //支付宝网关
    @Value("${alipay.server_url}")
    private String serverUrl;
    //支付宝分配给开发者的应用ID
    @Value("${alipay.app_id}")
    private String appId;
    //商户私钥
    @Value("${alipay.private_key}")
    private String privateKey;
    //字符编码格式
    private String charset = "UTF-8";
    //支付宝公钥
    @Value("${alipay.alipay_public_key}")
    private String alipayPublicKey;
    //签名方式
    private String signType = "RSA2";
    //服务器[异步通知]页面路径，支付宝服务器主动通知商户服务器里指定的页面http/https路径，告诉支付成功的信息
    @Value("${alipay.notify_url}")
    private  String notifyUrl;
    //页面跳转同步通知页面路径，支付成功，一般跳转到成功页
    @Value("${alipay.return_url}")
    private  String returnUrl;
    //销售产品码
    private String productCode = "FAST_INSTANT_TRADE_PAY";

    @PostMapping("/alipay")
    public String pay(@RequestBody PayTo to) throws AlipayApiException {
        //根据支付宝的配置生成一个支付客户端
        AlipayClient alipayClient = new DefaultAlipayClient(serverUrl, appId, privateKey,
                "json", charset, alipayPublicKey, signType);

        //创建一个支付请求
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();

        //设置请求参数
        //异步接收地址，仅支持http/https，公网可访问
        request.setNotifyUrl(notifyUrl);
        //同步跳转地址，仅支持http/https
        request.setReturnUrl(returnUrl);

        //设置必传参数
        JSONObject bizContent = new JSONObject();
        //商户订单号，商家自定义，保持唯一性
        bizContent.put("out_trade_no", to.getOutTradeNo());
        //支付金额，最小值0.01元
        bizContent.put("total_amount", to.getTotalAmount());
        //订单标题，不可使用特殊符号
        bizContent.put("subject", to.getSubject());
        //电脑网站支付场景固定传值FAST_INSTANT_TRADE_PAY
        bizContent.put("product_code", productCode);
        //订单包含的商品列表信息，json格式
        bizContent.put("goods_detail", to.getGoodsDetail());
        //订单绝对超时时间
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime date = LocalDateTime.now().plusMinutes(1);
        bizContent.put("time_expire", formatter.format(date));
        request.setBizContent(bizContent.toString());

        //响应为表单格式，可嵌入页面
        AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
        if (response.isSuccess()) {
            System.out.println("调用成功");
            return response.getBody();
        } else {
            System.out.println("调用失败");
            return response.getSubMsg();
        }
    }
}
