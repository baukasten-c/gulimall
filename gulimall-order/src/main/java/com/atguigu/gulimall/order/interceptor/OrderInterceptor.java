package com.atguigu.gulimall.order.interceptor;

import com.alibaba.nacos.common.packagescan.resource.AntPathMatcher;
import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.to.MemberRespTo;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class OrderInterceptor implements HandlerInterceptor {
    public static ThreadLocal<MemberRespTo> loginUser = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //放行查询订单状态方法
        String requestURI = request.getRequestURI();
        boolean match = new AntPathMatcher().match("/order/order/status/**", requestURI);
        if(match){
            return true;
        }

        MemberRespTo memberRespTo = (MemberRespTo) request.getSession().getAttribute(AuthServerConstant.LOGIN_USER);
        if(memberRespTo != null){ //登录，保存用户信息
            loginUser.set(memberRespTo);
            return true;
        }else{ //未登录，跳转登录页面
            request.getSession().setAttribute("msg", "请登录后再进行结算");
            response.sendRedirect("http://auth.gulimall.com/login.html");
            return false;
        }
    }
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex){
        loginUser.remove();
    }
}
