package com.atguigu.gulimall.order.dao;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author chen
 * @email 946952958@qq.com
 * @date 2023-07-14 17:18:32
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
	
}
