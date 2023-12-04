package com.atguigu.gulimall.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.gulimall.search.constant.EsConstant;
import com.atguigu.gulimall.search.service.ElasticSaveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ElasticSaveServiceImpl implements ElasticSaveService {
    @Autowired
    private ElasticsearchClient elasticsearchClient;

    //保存数据到es
    @Override
    public boolean productStatesUp(List<SkuEsModel> skuEsModels) throws IOException {
        //提前在es中建立索引(product)，建立好映射关系
        //在es中批量保存数据
        BulkRequest.Builder br = new BulkRequest.Builder();
        for (SkuEsModel skuEsModel : skuEsModels) {
            br.operations(op -> op
                    .index(idx -> idx
                            .index(EsConstant.PRODUCT_INDEX)
                            .id(skuEsModel.getSkuId().toString())
                            .document(skuEsModel)
                    )
            );
        }
        BulkResponse result = elasticsearchClient.bulk(br.build());
        if (result.errors()) {
            List<String> collect = result.items().stream().map(BulkResponseItem::id).collect(Collectors.toList());
            log.error("商品上架错误：{}", collect);
            return false;
        }
        return true;
    }
}
