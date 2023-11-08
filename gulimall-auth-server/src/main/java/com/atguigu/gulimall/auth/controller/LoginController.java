package com.atguigu.gulimall.auth.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.exception.BizCodeEnum;
import com.atguigu.common.to.MemberRespTo;
import com.atguigu.common.to.UserLoginTo;
import com.atguigu.common.to.UserRegistTo;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.auth.feign.MemberFeignService;
import com.atguigu.gulimall.auth.feign.ThirdPartFeignService;
import com.atguigu.gulimall.auth.vo.UserLoginVo;
import com.atguigu.gulimall.auth.vo.UserRegistVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
public class LoginController {
    @Autowired
    private ThirdPartFeignService thirdPartFeignService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private MemberFeignService memberFeignService;

    //发送验证码
    @GetMapping("/sms/sendcode")
    @ResponseBody
    public R sendCode(@RequestParam("mobile") String mobile){
        //验证码的再次校验
        String redisCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + mobile);
        if(StringUtils.hasLength(redisCode)){
            long l = Long.parseLong(redisCode.split("_")[1]);
            //60秒内不能再次发送验证码
            if(System.currentTimeMillis() - l < 60000){
                return R.error(BizCodeEnum.SMS_CODE_EXCEPTION.getCode(), BizCodeEnum.SMS_CODE_EXCEPTION.getMsg());
            }
        }
        //redis缓存验证码，防止同一个手机号在60秒内再次发送验证码
//        String code = UUID.randomUUID().toString().substring(0, 5);
        Random random = new Random();
        //int randomNumber = random.nextInt(MAX - MIN + 1) + MIN;
        String code = String.valueOf(random.nextInt(900000) + 100000);
        redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX + mobile,
                code + "_" + System.currentTimeMillis(),
                10, TimeUnit.MINUTES);
        thirdPartFeignService.sendCode(mobile, code);
        return R.ok();
    }

    //注册
    @PostMapping("/regist")
    public String regist(@Validated UserRegistVo vo, BindingResult result, RedirectAttributes redirectAttributes){
        //校验出错，返回注册页
        if(result.hasErrors()){
            Map<String, String> errors = result.getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
            //重定向携带数据，利用session原理(将数据放在session中，只要跳到下一个页面取出这个数据以后，session里面的数据就会被删除)
            redirectAttributes.addFlashAttribute("errors", errors);
            //不能使用"forward:/reg.html"，因为路径映射默认为GET，会报错Request method 'POST' not supported
            //不使用“reg”，因为用转发，刷新就相当于提交表单
            //需要写直接的域名http://auth.gulimall.com/，否则数据不结果nginx，无法进行渲染
            return "redirect:http://auth.gulimall.com/reg.html";
        }
        //校验验证码
        String code = vo.getCode();
        String mobile = vo.getMobile();
        String redisCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + mobile);
        if(StringUtils.hasLength(redisCode) && code.equals(redisCode.split("_")[0])){
            //删除验证码
            redisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + mobile);
            //校验通过，调用远程服务进行注册
            UserRegistTo to = new UserRegistTo();
            BeanUtils.copyProperties(vo, to);
            R r = memberFeignService.register(to);
            if(r.getCode() == 0){
                //注册成功，返回登录页
                return "redirect:http://auth.gulimall.com/login.html";
            }else{
                Map<String, String> errors = new HashMap<>();
                errors.put("msg", r.get("msg").toString());
                redirectAttributes.addFlashAttribute("errors", errors);
                return "redirect:http://auth.gulimall.com/reg.html";
            }
        }else{ //没有验证码或验证码过期
            Map<String, String> errors = new HashMap<>();
            errors.put("code", "验证码错误");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/reg.html";
        }
    }

    @GetMapping("/login.html")
    public String loginPage(HttpSession session){
        Object loginUser = session.getAttribute(AuthServerConstant.LOGIN_USER);
        if(loginUser != null){
            return "redirect:http://gulimall.com";
        }else{
            return "login";
        }
    }

    //登录
    @PostMapping("/login")
    public String login(UserLoginVo vo, RedirectAttributes redirectAttributes, HttpSession session){
        UserLoginTo to = new UserLoginTo();
        BeanUtils.copyProperties(vo, to);
        R r = memberFeignService.login(to);
        if(r.getCode() == 0){
            String json = JSON.toJSONString(r.get("data"));
            MemberRespTo loginUser = JSON.parseObject(json, MemberRespTo.class);
            session.setAttribute(AuthServerConstant.LOGIN_USER, loginUser);
            //登录成功，返回商城首页
            return "redirect:http://gulimall.com";
        }else{
            Map<String, String> errors = new HashMap<>();
            errors.put("msg", r.get("msg").toString());
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/login.html";
        }
    }
}
