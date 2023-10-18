package com.atguigu.common.exception;

public enum BizCodeEnum {
    UNKNOW_EXCEPTION(10000, "系统未知错误"),
    VALID_EXCEPTION(10001, "校验数据有误"),
    PRODUCT_UP_EXCEPTION(11000, "商品上架异常");
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
