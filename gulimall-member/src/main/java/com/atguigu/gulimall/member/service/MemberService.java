package com.atguigu.gulimall.member.service;

import com.atguigu.common.to.SocialUserTo;
import com.atguigu.gulimall.member.exception.MobileExistException;
import com.atguigu.gulimall.member.exception.UsernameExistException;
import com.atguigu.gulimall.member.vo.MemberLoginVo;
import com.atguigu.gulimall.member.vo.MemberRegistVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.member.entity.MemberEntity;

import java.util.Map;

/**
 * 会员
 *
 * @author chen
 * @email 946952958@qq.com
 * @date 2023-07-14 17:10:47
 */
public interface MemberService extends IService<MemberEntity> {
    PageUtils queryPage(Map<String, Object> params);

    void register(MemberRegistVo vo);

    void checkUserNameUnique(String userName) throws UsernameExistException;

    void checkMobileUnique(String mobile) throws MobileExistException;

    MemberEntity login(MemberLoginVo vo);

    MemberEntity login(SocialUserTo to);
}

