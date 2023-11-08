package com.atguigu.gulimall.member.controller;

import java.util.Arrays;
import java.util.Map;

import com.atguigu.common.exception.BizCodeEnum;
import com.atguigu.common.to.SocialUserTo;
import com.atguigu.gulimall.member.exception.MobileExistException;
import com.atguigu.gulimall.member.exception.UsernameExistException;
import com.atguigu.gulimall.member.feign.CouponFeignService;
import com.atguigu.gulimall.member.vo.MemberLoginVo;
import com.atguigu.gulimall.member.vo.MemberRegistVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.atguigu.gulimall.member.entity.MemberEntity;
import com.atguigu.gulimall.member.service.MemberService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.R;



/**
 * 会员
 *
 * @author chen
 * @email 946952958@qq.com
 * @date 2023-07-14 17:10:47
 */
@RestController
@RequestMapping("member/member")
public class MemberController {
    @Autowired
    private MemberService memberService;
    @Autowired
    CouponFeignService couponFeignService;

    //注册
    @PostMapping("/register")
    public R register(@RequestBody MemberRegistVo vo) {
        try{
            memberService.register(vo);
        }catch(UsernameExistException e){
            return R.error(BizCodeEnum.USERNAME_EXIST_EXCEPTION.getCode(), BizCodeEnum.USERNAME_EXIST_EXCEPTION.getMsg());
        }catch(MobileExistException e){
            return R.error(BizCodeEnum.MOBILE_EXIST_EXCEPTION.getCode(), BizCodeEnum.MOBILE_EXIST_EXCEPTION.getMsg());
        }
        return R.ok();
    }

    //密码登录
    @PostMapping("/login")
    public R login(@RequestBody MemberLoginVo vo){
        MemberEntity member = memberService.login(vo);
        if(member != null){
            return R.ok().put("data", member);
        }
        return R.error(BizCodeEnum.LOGIN_EXCEPTION.getCode(), BizCodeEnum.LOGIN_EXCEPTION.getMsg());
    }
    //gitee登录
    @PostMapping("/oauth2/login")
    public R login(@RequestBody SocialUserTo to){
        MemberEntity member = memberService.login(to);
        if(member != null){
            return R.ok().put("data", member);
        }
        return R.error(BizCodeEnum.LOGIN_EXCEPTION.getCode(), BizCodeEnum.LOGIN_EXCEPTION.getMsg());
    }

    @RequestMapping("/coupons")
    public R test(){
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setNickname("张三");
        R membercoupons = couponFeignService.membercoupons();
        return R.ok().put("member", memberEntity).put("coupons", membercoupons.get("coupons"));
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("member:member:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = memberService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("member:member:info")
    public R info(@PathVariable("id") Long id){
            MemberEntity member = memberService.getById(id);

        return R.ok().put("member", member);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("member:member:save")
    public R save(@RequestBody MemberEntity member){
            memberService.save(member);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("member:member:update")
    public R update(@RequestBody MemberEntity member){
            memberService.updateById(member);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("member:member:delete")
    public R delete(@RequestBody Long[] ids){
            memberService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
