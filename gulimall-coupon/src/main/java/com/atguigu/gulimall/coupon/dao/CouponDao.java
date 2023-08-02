package com.atguigu.gulimall.coupon.dao;

import com.atguigu.gulimall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author chen
 * @email 946952958@qq.com
 * @date 2023-07-14 16:54:58
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
