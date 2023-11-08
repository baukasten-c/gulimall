package com.atguigu.common.to;

import lombok.Data;

@Data
public class SocialUserTo {
    /**
     * 令牌
     */
    private String accessToken;
    /**
     * 令牌类型
     */
    private String tokenType;
    /**
     * 令牌过期时间
     */
    private Long expiresIn;
    /**
     * 刷新令牌
     */
    private String refreshToken;
    /**
     * 授权范围
     */
    private String scope;
    /**
     * 访问令牌的创建时间戳
     */
    private Long createdAt;
    /**
     * 该社交用户的唯一标识
     */
    private String id;
    /**
     * 用户用户名
     */
    private String login;
    /**
     * 用户昵称
     */
    private String name;
    /**
     * 用户头像
     */
    private String  avatarUrl;
    /**
     * 用户自我介绍
     */
    private String bio;
    /**
     * 用户邮箱
     */
    private String email;
}
