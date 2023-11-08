package com.atguigu.gulimall.auth.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.to.SocialUserTo;
import com.atguigu.common.utils.HttpUtils;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.auth.feign.MemberFeignService;
import com.atguigu.common.to.MemberRespTo;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Controller
public class OAuth2Controller { //处理社交登录请求
    @Value("${gitee.oauth.client-id}")
    private String clientId;
    @Value("${gitee.oauth.client-secret}")
    private String clientSecret;
    @Value("${gitee.oauth.redirect-uri}")
    private String redirectUri;
    @Autowired
    private MemberFeignService memberFeignService;

    @GetMapping("/oauth2/gitee/success")
    public String gitee(@RequestParam("code") String code, HttpSession session) throws Exception {
        Map<String, String> headers = new HashMap<>();
        Map<String, String> querys = new HashMap<>();
        querys.put("grant_type", "authorization_code");
        querys.put("code", code);
        querys.put("client_id", clientId);
        querys.put("redirect_uri", redirectUri);
        querys.put("client_secret", clientSecret);
        Map<String, String> bodys = new HashMap<>();
        //根据code换取accessToken
        HttpResponse responsePost = HttpUtils.doPost("https://gitee.com", "/oauth/token", "POST", headers, querys, bodys);
        if(responsePost.getStatusLine().getStatusCode() == 200){
            //获取accessToken
            String json = EntityUtils.toString(responsePost.getEntity());
            SocialUserTo socialUserTo = JSON.parseObject(json, SocialUserTo.class);
            //查询用户信息
            if(socialUserTo != null && StringUtils.hasLength(socialUserTo.getAccessToken())){
                querys.clear();
                querys.put("access_token", socialUserTo.getAccessToken());
                HttpResponse responseGet = HttpUtils.doGet("https://gitee.com", "/api/v5/user", "GET", headers, querys);
                if(responseGet.getStatusLine().getStatusCode() == 200){
                    //获取用户信息
                    json = EntityUtils.toString(responseGet.getEntity());
                    SocialUserTo user = JSON.parseObject(json, SocialUserTo.class);
                    BeanUtils.copyProperties(user, socialUserTo, "accessToken", "tokenType", "expiresIn", "refreshToken", "scope", "createdAt");
                    //当前用户第一次使用网站，自动注册(生成会员信息账号)
                    R r = memberFeignService.login(socialUserTo);
                    if(r.getCode() == 0){
                        MemberRespTo loginUser = r.getData("data", new TypeReference<MemberRespTo>(){});
                        session.setAttribute(AuthServerConstant.LOGIN_USER, loginUser);
                        //登录成功，跳转商城首页
                        return "redirect:http://gulimall.com";
                    }
                }
            }
        }
        //未成功获取到accessToken\未成功获取用户信息\未成功登录
        return "redirect:http://auth.gulimall.com/login.html";
    }
}
