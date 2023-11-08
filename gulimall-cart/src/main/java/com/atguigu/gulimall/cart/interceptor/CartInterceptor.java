package com.atguigu.gulimall.cart.interceptor;

import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.constant.CartConstant;
import com.atguigu.common.to.MemberRespTo;
import com.atguigu.common.to.UserInfoTo;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

public class CartInterceptor implements HandlerInterceptor {
    //使用ThreadLocal保证同一个线程数据共享
    public static ThreadLocal<UserInfoTo> threadLocal = new ThreadLocal<>();

    //执行目标方法前：判断用户登录状态，并封装传递给controller目标请求
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        UserInfoTo userInfoTo = new UserInfoTo();
        MemberRespTo loginUser = (MemberRespTo) request.getSession().getAttribute(AuthServerConstant.LOGIN_USER);
        //登录则标记为用户，有userId和userKey；未登录则标记为临时用户，只有userKey
        if(loginUser != null){
            userInfoTo.setUserId(loginUser.getId());
        }
        Cookie[] cookies = request.getCookies();
        if(cookies != null && cookies.length > 0){
            //使用Optional可以更好地处理可能不存在的情况，避免空指针异常
            Optional<Cookie> tempUserCookie = Arrays.stream(cookies)
                    .filter(cookie -> CartConstant.TEMP_USER_COOKIE_NAME.equals(cookie.getName()))
                    //如果找到匹配的元素，findFirst()返回一个包含该元素的Optional对象，否则返回一个空的Optional
                    .findFirst();
            tempUserCookie.ifPresent(cookie -> {
                userInfoTo.setUserKey(cookie.getValue());
            });
        }
        //第一次使用，分配临时用户
        if(!StringUtils.hasLength(userInfoTo.getUserKey())){
            String uuid = UUID.randomUUID().toString();
            userInfoTo.setUserKey(uuid);
            userInfoTo.setTempUser(true);
        }
        threadLocal.set(userInfoTo);
        return true;
    }

    //执行目标方法后：浏览器保存临时用户
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        UserInfoTo userInfoTo = threadLocal.get();
        //避免临时用户存储时间无限延长
        if(!userInfoTo.isTempUser()) {
            //创建一个cookie，用于保存临时用户
            Cookie cookie = new Cookie(CartConstant.TEMP_USER_COOKIE_NAME, userInfoTo.getUserKey());
            //扩大作用域
            cookie.setDomain("gulimall.com");
            //设置过期时间(1个月)
            cookie.setMaxAge(CartConstant.TEMP_USER_COOKIE_TIMEOUT);
            response.addCookie(cookie);
        }
    }

    //视图渲染完成后：释放资源
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //使用的是tomcat线程池，请求结束后，线程不会结束
        //如果不手动删除线程变量，可能会导致内存泄漏
        threadLocal.remove();
    }
}
