package com.atguigu.gulimall.product.web;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catelog2Vo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class IndexController {
    @Autowired
    CategoryService categoryService;

    //页面跳转
    @GetMapping({"/","/index.html"})
    public String indexPage(Model model){
        //查询所有一级分类菜单
        List<CategoryEntity> categoryEntities = categoryService.getLevel1Categorys();
        model.addAttribute("categorys", categoryEntities);
        //通过视图解析器进行拼串：classpath:/templates/ + 返回值 + .html
        return "index";
    }

    //获取三级分类菜单
    @ResponseBody
    @GetMapping("/index/catelog.json")
    public Map<String, List<Catelog2Vo>> getCatelogJson(){
        Map<String, List<Catelog2Vo>> map = categoryService.getCatelogJson();
        return map;
    }

    //性能测试，简单服务
    @ResponseBody
    @GetMapping("/hello")
    public String hello(){
        return "hello";
    }
}
