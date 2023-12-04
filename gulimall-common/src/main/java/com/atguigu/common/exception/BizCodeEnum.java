package com.atguigu.common.exception;

public enum BizCodeEnum {
    UNKNOW_EXCEPTION(10000, "系统未知错误"),
    VALID_EXCEPTION(10001, "校验数据有误"),
    SMS_CODE_EXCEPTION(10002, "验证码获取次数太多，请稍后再试"),
    PRODUCT_UP_EXCEPTION(11000, "商品上架异常"),
    USERNAME_EXIST_EXCEPTION(15001, "用户名存在"),
    MOBILE_EXIST_EXCEPTION(15002, "手机号存在"),
    LOGIN_EXCEPTION(15003, "账号密码存在错误"),
    NO_STOCK_EXCEPTION(16000,"下单失败，商品库存不足，请确认后再次提交");
    private int code;
    private String msg;

    BizCodeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
