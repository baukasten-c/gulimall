package com.atguigu.gulimall.member.service.impl;

import com.atguigu.common.constant.MemberConstant;
import com.atguigu.common.to.SocialUserTo;
import com.atguigu.gulimall.member.dao.MemberLevelDao;
import com.atguigu.gulimall.member.entity.MemberLevelEntity;
import com.atguigu.gulimall.member.exception.MobileExistException;
import com.atguigu.gulimall.member.exception.UsernameExistException;
import com.atguigu.gulimall.member.vo.MemberLoginVo;
import com.atguigu.gulimall.member.vo.MemberRegistVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.member.dao.MemberDao;
import com.atguigu.gulimall.member.entity.MemberEntity;
import com.atguigu.gulimall.member.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {
    @Autowired
    private MemberLevelDao memberLevelDao;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );
        return new PageUtils(page);
    }

    @Override
    public void register(MemberRegistVo vo) {
        MemberEntity member = new MemberEntity();
        //设置默认会员等级
        MemberLevelEntity memberLevelEntity = memberLevelDao.getDefaultLevel();
        member.setLevelId(memberLevelEntity.getId());
        //校验用户名、手机号是否唯一
        checkUserNameUnique(vo.getUserName());
        checkMobileUnique(vo.getMobile());
        member.setUsername(vo.getUserName());
        member.setMobile(vo.getMobile());
        member.setNickname(vo.getUserName());
        //密码加密(MD5加盐)
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encode = passwordEncoder.encode(vo.getPassword());
        member.setPassword(encode);
        this.baseMapper.insert(member);
    }

    @Override
    public void checkUserNameUnique(String userName) {
        Long count = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("username", userName));
        if(count > 0){
            throw new UsernameExistException();
        }
    }

    @Override
    public void checkMobileUnique(String mobile) {
        Long count = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", mobile));
        if(count > 0){
            throw new MobileExistException();
        }
    }

    //密码登录
    @Override
    public MemberEntity login(MemberLoginVo vo) {
        String loginAcct = vo.getLoginAcct();
        String password = vo.getPassword();
        MemberEntity member = this.baseMapper.selectOne(new QueryWrapper<MemberEntity>()
                .eq("username", loginAcct).or().eq("mobile", loginAcct));
        if(member != null){
            String passwordDb = member.getPassword();
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            boolean matches = passwordEncoder.matches(password, passwordDb);
            //密码匹配，登录成功
            if(matches){
                return  member;
            }
        }
        return null;
    }

    //gitee登录
    @Override
    public MemberEntity login(SocialUserTo to) {
        String uid = to.getId();
        MemberEntity member = this.baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("social_uid", uid));
        if(member == null){ //用户未注册
            member = new MemberEntity();
            member.setUsername(to.getLogin());
            member.setNickname(to.getName());
            member.setEmail(to.getEmail());
            member.setHeader(to.getAvatarUrl());
            member.setSign(to.getBio());
            member.setSocialUid(uid);
            //设置默认会员等级
            MemberLevelEntity memberLevelEntity = memberLevelDao.getDefaultLevel();
            member.setLevelId(memberLevelEntity.getId());
            this.baseMapper.insert(member);
        }
        redisTemplate.opsForValue().set(MemberConstant.OAUTH_GITEE + uid, to.getAccessToken(), to.getExpiresIn(), TimeUnit.SECONDS);
        return member;
    }
}