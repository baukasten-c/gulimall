package com.atguigu.gulimall.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.termvectors.Term;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class GulimallSearchApplicationTests {
    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Test
    public void contextLoads() {
        System.out.println(elasticsearchClient);
    }

    @Test
    public void indexData() throws IOException {
        User user = new User("张三", "男", 20);
        IndexResponse indexResponse = elasticsearchClient.index(i -> i
                .index("users")
                //设置id
                .id("1")
                //传入user对象
                .document(user));
        System.out.println(indexResponse);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    class User{
        private String userName;
        private String gender;
        private Integer age;
    }

    @Test
    public void searchData() throws IOException {
        SearchResponse<Bank> response = elasticsearchClient.search(s -> s
                .index("bank")
                .query(q -> q
                        .match(t -> t
                                .field("address")
                                .query("mill")
                        )
                )
                .aggregations("ageAgg", a -> a
                        .terms(h -> h
                                .field("age")
                                .size(10)
                        )
                )
                .aggregations("balanceAvg", a -> a
                        .avg(h -> h
                                .field("balance")
                        )
                ), Bank.class
        );
//        System.out.println(response);
        List<Hit<Bank>> hits = response.hits().hits();
        for (Hit<Bank> hit: hits) {
            Bank bank = hit.source();
//            System.out.println("bank：" + bank);
        }
        List<LongTermsBucket> ageAgg = response.aggregations().get("ageAgg").lterms().buckets().array();
        for (LongTermsBucket bucket: ageAgg) {
            System.out.println("年龄：" + bucket.key() + "===>" + bucket.docCount());
        }
        AvgAggregate balanceAvg = response.aggregations().get("balanceAvg").avg();
        System.out.println("平均薪资：" + balanceAvg.value());
    }

    @Data
    static class Bank {
        @JsonProperty("account_number")
        private long accountNumber;
        private String address;
        private int age;
        private long balance;
        private String city;
        private String email;
        private String employer;
        private String firstname;
        private String gender;
        private String lastname;
        private String state;
    }
}
