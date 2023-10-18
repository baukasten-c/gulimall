package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.atguigu.gulimall.product.vo.Catelog2Vo;
import com.sun.xml.internal.bind.v2.TODO;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
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
import org.springframework.util.StringUtils;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {
    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

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

    //查询所有一级分类菜单
    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        List<CategoryEntity> categoryEntities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
        return categoryEntities;
    }

    //查询子菜单
    private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList, Long parent_cid) {
        return selectList.stream().filter(item -> item.getParentCid() == parent_cid).collect(Collectors.toList());
    }

    //获取三级分类菜单
    /*@Override
    public Map<String, List<Catelog2Vo>> getCatelogJson() {
        //查出所有一级分类菜单
        List<CategoryEntity> level1Categorys = getLevel1Categorys();
        //封装数据
        Map<String, List<Catelog2Vo>> categorys = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //查询一级分类菜单的二级分类菜单
            List<CategoryEntity> level2Categorys = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", v.getCatId()));
            //封装二级分类菜单
            List<Catelog2Vo> catelog2Vos = null;
            if (level2Categorys != null) {
                catelog2Vos = level2Categorys.stream().map(l2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null,
                            l2.getCatId().toString(), l2.getName().toString());
                    //查询二级分类的三级分类菜单
                    List<CategoryEntity> level3Categorys = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", l2.getCatId()));
                    //封装三级分类菜单
                    if (level3Categorys != null) {
                        List<Catelog2Vo.Catelog3Vo> catelog3Vos = level3Categorys.stream().map(l3 -> {
                            Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(),
                                    l3.getCatId().toString(), l3.getName());
                            return catelog3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatelog3List(catelog3Vos);
                    }
                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            return catelog2Vos;
        }));
        return categorys;
    }*/
    //优化一：将数据库多次查询变为一次
    public Map<String, List<Catelog2Vo>> getCatelogJsonFromDb() {
        //得到锁以后，首先去缓存中确定一次，如果没有才需要继续查询
        String jsonString = redisTemplate.opsForValue().get("categorys");
        //缓存不为空直接返回
        if(StringUtils.hasLength(jsonString)){
            Map<String, List<Catelog2Vo>> result = JSON.parseObject(jsonString, new TypeReference<Map<String, List<Catelog2Vo>>>(){});
            return result;
        }
        //查出数据库中所有菜单
        List<CategoryEntity> selectList = baseMapper.selectList(null);
        //查出所有一级分类菜单
        List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);
        //封装数据
        Map<String, List<Catelog2Vo>> categorys = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //查询一级分类菜单的二级分类菜单
            List<CategoryEntity> level2Categorys = getParent_cid(selectList, v.getCatId());
            //封装二级分类菜单
            List<Catelog2Vo> catelog2Vos = null;
            if (level2Categorys != null) {
                catelog2Vos = level2Categorys.stream().map(l2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null,
                            l2.getCatId().toString(), l2.getName().toString());
                    //查询二级分类菜单的三级分类菜单
                    List<CategoryEntity> level3Categorys = getParent_cid(selectList, l2.getCatId());
                    //封装三级分类菜单
                    if (level3Categorys != null) {
                        List<Catelog2Vo.Catelog3Vo> catelog3Vos = level3Categorys.stream().map(l3 -> {
                            Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(),
                                    l3.getCatId().toString(), l3.getName());
                            return catelog3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatelog3List(catelog3Vos);
                    }
                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            return catelog2Vos;
        }));
        //加载到的数据转为JSON字符串，存入缓存
        jsonString = JSON.toJSONString(categorys);
        redisTemplate.opsForValue().set("categorys", jsonString);
        return categorys;
    }
    //优化二：添加缓存
    @Override
    public Map<String, List<Catelog2Vo>> getCatelogJson() {
        //从缓存加载数据，缓存中存放的数据是JSON字符串(JSON跨语言，跨平台兼容)
        String categorys = redisTemplate.opsForValue().get("categorys");
        //缓存中没有，从数据库加载数据
        if(!StringUtils.hasLength(categorys)){
            Map<String, List<Catelog2Vo>> catelogJsonFromDb = getCatelogJsonFromDbWithRedissonLock();
            return catelogJsonFromDb;
        }
        //TypeReference的构造器为protected类型，需要使用子类来创建新的对象实例，或匿名内部类
        Map<String, List<Catelog2Vo>> result = JSON.parseObject(categorys, new TypeReference<Map<String, List<Catelog2Vo>>>(){});
        return result;
    }
    //优化三：添加分布式锁
    public Map<String, List<Catelog2Vo>> getCatelogJsonFromDbWithRedisLock() {
        String uuid = UUID.randomUUID().toString();
        //占锁，设置过期时间和加锁同步，保证原子性，避免死锁
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid,300, TimeUnit.SECONDS);
        if (lock) {
            //加锁成功，执行业务
            Map<String, List<Catelog2Vo>> dataFromDb = null;
            try {
                dataFromDb = getCatelogJsonFromDb();
            } finally {
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                //释放锁
                redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList("lock"), uuid);
            }
            return dataFromDb;
        } else {
            //加锁失败，重试
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return getCatelogJsonFromDbWithRedisLock();
        }
    }
    //优化四：Redisson
    //缺点：不能保证缓存数据与数据库中数据一致。可以使用双写模式、失效模式优化。仍可能出现缓存不一致的情况，可以加读写锁
    public Map<String, List<Catelog2Vo>> getCatelogJsonFromDbWithRedissonLock(){
        //名字相同即为同一把锁，锁的粒度，越细越快。具体缓存的是某个数据，11号商品：product-11-lock
        RLock lock = redissonClient.getLock("catalogJson-lock");
        Map<String, List<Catelog2Vo>> dataFromDb = null;
        try {
            lock.lock();
            dataFromDb = getCatelogJsonFromDb();
        } finally {
            lock.unlock();
        }
        return dataFromDb;
    }
}