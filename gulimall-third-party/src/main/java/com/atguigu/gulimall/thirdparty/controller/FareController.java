package com.atguigu.gulimall.thirdparty.controller;

import com.atguigu.common.constant.ThirdPartyConstant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("thirdparty/kuaidi100")
public class FareController {
    @Value("${kuaidi100.fare.secret-key}")
    private String secretKey;
    @Value("${kuaidi100.fare.secret-code}")
    private String secretCode;
    @Value("${kuaidi100.fare.secret-sign}")
    private String secretSign;
    @Value("${kuaidi100.fare.sendAddr}")
    private String sendAddr;

    @PostMapping ("/fare")
    public String post(@RequestParam("receiveAddr") String receiveAddr, @RequestParam("weight") String weight) {
        Map params = new HashMap();
        params.put("secret_key", secretKey);
        params.put("secret_code", secretCode);
        params.put("secret_sign", secretSign);
        params.put("companyName", ThirdPartyConstant.COMPANY_NAME);
        params.put("sendAddr", sendAddr);
        params.put("receiveAddr", receiveAddr);
        params.put("weight", weight);
        StringBuilder response = new StringBuilder("");
        BufferedReader reader = null;
        try {
            StringBuilder builder = new StringBuilder();
            Set<Map.Entry<String, String>> entrySet = params.entrySet();
            for (Map.Entry<String, String> param : entrySet) {
                if (builder.length() > 0) {
                    builder.append('&');
                }
                builder.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                builder.append('=');
                builder.append(URLEncoder.encode(param.getValue(), "UTF-8"));
            }
            byte[] bytes = builder.toString().getBytes("UTF-8");
            URL url = new URL("http://cloud.kuaidi100.com/api");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", String.valueOf(bytes.length));
            conn.setDoOutput(true);
            conn.getOutputStream().write(bytes);
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String line = "";
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != reader) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return response.toString();
    }
}