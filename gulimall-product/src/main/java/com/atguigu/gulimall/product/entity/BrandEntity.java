package com.atguigu.gulimall.product.entity;

import com.atguigu.common.validator.ListValue;
import com.atguigu.common.validator.group.AddGroup;
import com.atguigu.common.validator.group.UpdateGroup;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * 品牌
 * 
 * @author chen
 * @email 946952958@qq.com
 * @date 2023-07-14 15:17:38
 */
@Data
@TableName("pms_brand")
public class BrandEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 品牌id
	 */
	@Null(message = "添加操作不需要品牌id", groups = {AddGroup.class})
	@NotNull(message = "修改操作必须要品牌id", groups = {UpdateGroup.class})
	@TableId
	private Long brandId;
	/**
	 * 品牌名
	 */
	@NotBlank(message = "品牌名不能为空")
	private String name;
	/**
	 * 品牌logo地址
	 */
	@NotBlank(message = "品牌logo不能为空")
	@URL(message = "品牌logo地址必须合法")
	private String logo;
	/**
	 * 介绍
	 */
	@NotBlank(message = "介绍不能为空")
	private String descript;
	/**
	 * 显示状态[0-不显示；1-显示]
	 */
	@NotNull(message = "显示状态不能为空")
	@ListValue(values = {0, 1})
	private Integer showStatus;
	/**
	 * 检索首字母
	 */
	@NotBlank(message = "检索首字母不能为空")
	@Pattern(regexp = "^[a-zA-Z]$", message = "检索首字母必须为a-z或A-Z之间的一个字母")
	private String firstLetter;
	/**
	 * 排序
	 */
	@NotNull(message = "排序不能为空")
	@Min(value = 0, message = "排序必须是正整数")
	private Integer sort;

}
