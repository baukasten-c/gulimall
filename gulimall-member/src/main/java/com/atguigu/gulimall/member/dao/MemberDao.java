package com.atguigu.gulimall.member.dao;

import com.atguigu.gulimall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author chen
 * @email 946952958@qq.com
 * @date 2023-07-14 17:10:47
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
