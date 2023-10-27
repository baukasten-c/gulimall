package com.atguigu.gulimall.search.controller;

import com.atguigu.gulimall.search.service.MallSearchService;
import com.atguigu.gulimall.search.vo.SearchParam;
import com.atguigu.gulimall.search.vo.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class SearchController {
    @Autowired
    private MallSearchService mallSearchService;
    @GetMapping({"/","/list.html"})
    //将所有页面可能提交来的请求参数封装为SearchParam对象，避免复制传参
    //Model用于页面显示
    public String listPage(SearchParam param, Model model, HttpServletRequest request){
        param.setQueryString(request.getQueryString());
        //根据页面查询参数，去es中检索商品
        SearchResult result = mallSearchService.search(param);
        model.addAttribute("result", result);
        return "list";
    }
}
