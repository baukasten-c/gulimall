package com.atguigu.gulimall.search.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GulimallSearchConfig {
    @Bean //注入IOC容器
    public ElasticsearchClient elasticsearchClient(){
        // 创建low-level client低级客户端
        RestClient restClient = RestClient.builder(new HttpHost("192.168.91.128", 9200)).build();
        // 使用 Jackson 映射器创建传输
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        // 并创建 API 客户端
        return new ElasticsearchClient(transport);
    }
}
