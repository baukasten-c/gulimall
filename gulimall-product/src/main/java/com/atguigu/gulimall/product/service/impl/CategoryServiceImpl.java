package com.atguigu.gulimall.product.service.impl;

import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.sun.xml.internal.bind.v2.TODO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {
    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    //查出所有菜单以及子菜单，以树形结构组装起来
    @Override
    public List<CategoryEntity> listWithTree() {
        //1、查出所有菜单
        List<CategoryEntity> entities = baseMapper.selectList(null);
        //2、组装成树形结构
        List<CategoryEntity> menus = entities.stream()
                .filter(categoryEntity -> categoryEntity.getParentCid() == 0) //找到所有的一级菜单
                .map(menu -> {
                    menu.setChildren(getChildrens(menu, entities)); //组装一级菜单的子菜单
                    return menu;
                }).sorted(Comparator.comparingInt(CategoryEntity::getSort)) //对一级菜单进行排序
                .collect(Collectors.toList()); //转为树形结构的List<CategoryEntity>
        return menus;
    }

    //递归查找所有菜单的子菜单
    private List<CategoryEntity> getChildrens(CategoryEntity root, List<CategoryEntity> all){
        List<CategoryEntity> children = all.stream()
                .filter(categoryEntity -> (categoryEntity.getParentCid()).equals(root.getCatId())) //查出root菜单的所有子菜单
                .map(categoryEntity -> { //递归
                    categoryEntity.setChildren(getChildrens(categoryEntity, all));
                    return categoryEntity;
                })
                //对子菜单进行排序，对空的sort属性设置为0
                .sorted(Comparator.comparingInt(category -> category.getSort() == null ? 0 : category.getSort()))
                .collect(Collectors.toList()); //转为树形结构的List<CategoryEntity>
        return children;
    }

    @Override
    public void removeMenuByIds(List<Long> list) {
        //1、检查当前删除的菜单，是否被别的地方引用
        //TODO
        //2、批量逻辑删除
        baseMapper.deleteBatchIds(list);
    }

    //根据叶子菜单获取完整分类菜单
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        findParentPath(catelogId, paths);
        return paths.toArray(new Long[paths.size()]);
    }

    private void findParentPath(Long catelogId, List<Long> paths){
        if(catelogId != 0){
            findParentPath(this.getById(catelogId).getParentCid(), paths);
            paths.add(catelogId);
        }
    }

    //级联更新所有关联的数据
    @Override
    @Transactional
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }
}