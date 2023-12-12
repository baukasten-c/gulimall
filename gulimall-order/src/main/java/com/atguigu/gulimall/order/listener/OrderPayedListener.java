package com.atguigu.gulimall.order.listener;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.vo.PayAsyncVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@RestController
public class OrderPayedListener {
    //支付宝公钥
    @Value("${alipay.alipay_public_key}")
    private String alipayPublicKey;

    @Autowired
    private OrderService orderService;

    //异步通知
    @PostMapping("/payed/notify")
    public String handlerAlipay(HttpServletRequest request, PayAsyncVo vo) throws AlipayApiException {
        //将异步通知中收到的所有参数都存放到map中
        Map<String, String> params = new HashMap<>();
        Map requestParams = request.getParameterMap();
        for(Iterator iter = requestParams.keySet().iterator(); iter.hasNext();){
            String name = (String) iter.next();
            String[] values = (String[]) requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
            }
            params.put(name, valueStr);
        }
        //验签，防止异步通知被伪造
        boolean signVerified = AlipaySignature.rsaCheckV1(params, alipayPublicKey, "UTF-8", "RSA2");
        if(signVerified){
            //验签成功，处理支付结果
            orderService.handlePayResult(vo);
            return "success";
        }else{
            //验签失败，支付宝未收到success，会继续发送异步通知
            return "error";
        }
    }
}
