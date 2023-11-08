package com.atguigu.gulimall.member.exception;

public class MobileExistException extends RuntimeException{
    public MobileExistException() {
        super("手机号存在");
    }
}
