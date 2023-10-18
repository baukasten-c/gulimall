package com.atguigu.gulimall.search.service;

import com.atguigu.common.to.es.SkuEsModel;

import java.io.IOException;
import java.util.List;

public interface ElasticSaveService {
    boolean productStatesUp(List<SkuEsModel> skuEsModels) throws IOException;
}
