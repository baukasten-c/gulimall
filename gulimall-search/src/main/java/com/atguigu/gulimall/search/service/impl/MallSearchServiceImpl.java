package com.atguigu.gulimall.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.gulimall.search.constant.EsConstant;
import com.atguigu.gulimall.search.service.MallSearchService;
import com.atguigu.gulimall.search.vo.SearchParam;
import com.atguigu.gulimall.search.vo.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static net.sf.jsqlparser.parser.feature.Feature.replace;

@Service
@Slf4j
public class MallSearchServiceImpl implements MallSearchService {
    @Autowired
    private ElasticsearchClient elasticsearchClient;

    //在es中进行检索
    @Override
    public SearchResult search(SearchParam param) {
        //准备索引请求
        SearchRequest searchRequest = buildSearchRequest(param);
        SearchResult searchResult = null;
        try {
            //执行索引请求
            SearchResponse<SkuEsModel> response = elasticsearchClient.search(searchRequest, SkuEsModel.class);
            //分析响应数据，封装为指定格式
            searchResult = buildSearchResult(response, param);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return searchResult;
    }

    //动态构建查询需要的DSL语句
    private SearchRequest buildSearchRequest(SearchParam param) {
        SearchRequest.Builder searchRequest = new SearchRequest.Builder();
        searchRequest.index(EsConstant.PRODUCT_INDEX);

        BoolQuery.Builder boolQuery = new BoolQuery.Builder();
        //数组\列表判断是否为null和size是否为0，字符串判断是否为null和是否为“”，数字判断是否为null
        //模糊匹配
        if(StringUtils.hasLength(param.getKeyword())){
            boolQuery.must(m -> m.match(t -> t.field("skuTitle").query(param.getKeyword())));
        }
        //过滤
        //按照三级分类id
        if(param.getCatelog3Id() != null){
            boolQuery.filter(f -> f.term(t -> t.field("catelogId").value(param.getCatelog3Id())));
        }
        //按照品牌id
        //!CollectionUtils.isEmpty同时判断数组\列表是否为null或为空，替代param.getBrandId() != null && param.getBrandId().size() > 0
        if(!CollectionUtils.isEmpty(param.getBrandId())){
            TermsQueryField termsQueryField = new TermsQueryField.Builder()
                    .value(param.getBrandId().stream().map(FieldValue::of).collect(Collectors.toList()))
                    .build();
            boolQuery.filter(f -> f.terms(t -> t.field("brandId").terms(termsQueryField)));
        }
        //按照属性：attrs=1_5寸:8寸&2_16G:8G
        if(!CollectionUtils.isEmpty(param.getAttrs())){
            for(String attrStr : param.getAttrs()){
                String[] s = attrStr.split("_");
                //检索的属性id
                String attrId = s[0];
                //检索的属性值
                String[] attrValue = s[1].split(":");
                List<FieldValue> attrValues = Arrays.stream(attrValue).map(FieldValue::of).collect(Collectors.toList());
                //每个属性都需要单独过滤
                boolQuery.filter(f -> f
                        .nested(n -> n
                                .path("attrs")
                                .query(q -> q
                                        .bool(b -> b
                                                .must(m -> m
                                                        .term(t -> t
                                                                .field("attrs.attrId")
                                                                .value(attrId)))
                                                .must(m -> m
                                                        .terms(t -> t
                                                                .field("attrs.attrValue")
                                                                .terms(ts -> ts.value(attrValues))))
                                        )
                                )
                        )
                );
            }
        }
        //按照是否有库存
        if(param.getHasStock() != null){
            boolQuery.filter(f -> f.term(t -> t.field("hasStock").value(param.getHasStock() == 1)));
        }
        //按照价格区间
        if(StringUtils.hasLength(param.getPrice())){
            String[] price = param.getPrice().split("_");
            RangeQuery.Builder rangeQuery = new RangeQuery.Builder().field("price");
            if(price.length == 1){
                rangeQuery.gte(JsonData.of(price[0]));
            }else if(price[0] == ""){
                rangeQuery.lte(JsonData.of(price[1]));
            }else{
                rangeQuery.gte(JsonData.of(price[0])).lte(JsonData.of(price[1]));
            }
            boolQuery.filter(f -> f.range(rangeQuery.build()));
        }
        searchRequest.query(q -> q.bool(boolQuery.build()));
        //排序
        if(StringUtils.hasLength(param.getSort())){
            String[] sort = param.getSort().split("_");
            SortOrder order = sort[1].equalsIgnoreCase("asc") ? SortOrder.Asc : SortOrder.Desc;
            searchRequest.sort(s -> s.field(f -> f.field(sort[0]).order(order)));
        }
        //分页
        searchRequest.from((param.getPageNum() - 1) * EsConstant.PRODUCT_PAGESIZE);
        searchRequest.size(EsConstant.PRODUCT_PAGESIZE);
        //高亮
        if(StringUtils.hasLength(param.getKeyword())){
            searchRequest.highlight(h -> h
                    .fields("skuTitle", f -> f
                            .preTags("<b style='color:red'>").postTags("</b>")));
        }
        //聚合分析
        //品牌聚合
        searchRequest.aggregations("brand_agg", a -> a
                .terms(t -> t.field("brandId").size(50))
                //子聚合
                .aggregations("brand_name_agg", a1 -> a1.terms(t1 -> t1.field("brandName").size(1)))
                .aggregations("brand_img_agg", a2 -> a2.terms(t2 -> t2.field("brandImg").size(1))));
        //分类聚合
        searchRequest.aggregations("catelog_agg", a -> a
                .terms(t -> t.field("catelogId").size(20))
                .aggregations("catelog_name_agg", a1 -> a1.terms(t1 -> t1.field("catelogName").size(1))));
        //属性聚合
        searchRequest.aggregations("resellers", a -> a
                .nested(n -> n.path("attrs"))
                .aggregations("attr_id_agg", a0 -> a0
                        .terms(t0 -> t0.field("attrs.attrId"))
                        .aggregations("attr_name_agg", a1 -> a1.terms(t1 -> t1.field("attrs.attrName").size(1)))
                        .aggregations("attr_value_agg", a2 -> a2.terms(t2 -> t2.field("attrs.attrValue").size(50)))));
        return searchRequest.build();
    }

    //构建结果数据
    private SearchResult buildSearchResult(SearchResponse response, SearchParam param){
        SearchResult searchResult = new SearchResult();
        //商品信息
        List<SkuEsModel> esModels = new ArrayList<>();
        List<Hit<SkuEsModel>> hits = response.hits().hits();
        if(!CollectionUtils.isEmpty(hits)){
            for(Hit<SkuEsModel> hit : hits){
                SkuEsModel esModel = hit.source();
                //判断是否按关键字检索，若是就显示高亮，否则不显示
                if (!StringUtils.isEmpty(param.getKeyword())) {
                    //拿到高亮信息显示标题
                    String skuTitleValue = hit.highlight().get("skuTitle").get(0);
                    esModel.setSkuTitle(skuTitleValue);
                }
                esModels.add(esModel);
            }
        }
        searchResult.setProduct(esModels);
        //品牌信息
        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        Aggregate brandAgg = (Aggregate) response.aggregations().get("brand_agg");
        List<LongTermsBucket> brandBuckets = brandAgg.lterms().buckets().array();
        for(LongTermsBucket bucket : brandBuckets){
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            //获取品牌id
            long id = bucket.key();
            brandVo.setBrandId(id);
            //获取品牌名
            String name = bucket.aggregations().get("brand_name_agg").sterms().buckets().array().get(0).key().stringValue();
            brandVo.setBrandName(name);
            //获取品牌图片
            String img = bucket.aggregations().get("brand_img_agg").sterms().buckets().array().get(0).key().stringValue();
            brandVo.setBrandImg(img);
            brandVos.add(brandVo);
        }
        searchResult.setBrands(brandVos);
        //分类信息
        List<SearchResult.CatelogVo> catelogVos = new ArrayList<>();
        Aggregate catelogAgg = (Aggregate) response.aggregations().get("catelog_agg");
        List<LongTermsBucket> CatelogBuckets = catelogAgg.lterms().buckets().array();
        for(LongTermsBucket bucket : CatelogBuckets){
            SearchResult.CatelogVo catelogVo = new SearchResult.CatelogVo();
            //获取分类id
            long id = bucket.key();
            catelogVo.setCatelogId(id);
            //获取分类名
            String name = bucket.aggregations().get("catelog_name_agg").sterms().buckets().array().get(0).key().stringValue();
            catelogVo.setCatelogName(name);
            catelogVos.add(catelogVo);
        }
        searchResult.setCatelogs(catelogVos);
        //属性信息
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        Aggregate resellers = (Aggregate) response.aggregations().get("resellers");
        List<LongTermsBucket> attrAgg = resellers.nested().aggregations().get("attr_id_agg").lterms().buckets().array();
        for(LongTermsBucket bucket : attrAgg){
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            //获取属性id
            long id = bucket.key();
            attrVo.setAttrId(id);
            //获取属性名
            String name = bucket.aggregations().get("attr_name_agg").sterms().buckets().array().get(0).key().stringValue();
            attrVo.setAttrName(name);
            //获取属性值
            List<String> value = bucket.aggregations().get("attr_value_agg").sterms().buckets().array()
                    .stream().map(item -> item.key().stringValue()).collect(Collectors.toList());
            attrVo.setAttrValue(value);
            attrVos.add(attrVo);
        }
        searchResult.setAttrs(attrVos);
        //分页信息
        //页码
        searchResult.setPageNum(param.getPageNum());
        //总记录数
        long total = response.hits().total().value();
        searchResult.setTotal(total);
        //总页码
        int totalPages = (int) (total % EsConstant.PRODUCT_PAGESIZE == 0 ? total / EsConstant.PRODUCT_PAGESIZE : (total / EsConstant.PRODUCT_PAGESIZE + 1));
        searchResult.setTotalPages(totalPages);
        //页码导航
        List<Integer> pageNavs = new ArrayList<>();
        for(int i = 1; i <= totalPages; i++){
            pageNavs.add(i);
        }
        searchResult.setPageNavs(pageNavs);
        //面包屑导航
        //品牌相关
        if(!CollectionUtils.isEmpty(param.getBrandId())){
            List<SearchResult.NavVo> navs = searchResult.getNavs();
            SearchResult.NavVo navVo = new SearchResult.NavVo();
            navVo.setNavName("品牌");
            List<SearchResult.BrandVo> brands = searchResult.getBrands().stream()
                    .filter(brandVo -> param.getBrandId().contains(brandVo.getBrandId()))
                    .collect(Collectors.toList());
            StringBuffer buffer = new StringBuffer();
            String replace = param.getQueryString();
            for(SearchResult.BrandVo brand : brands){
                buffer.append(brand.getBrandName() + ";");
                replace = replaceQueryString(replace, "brandId", brand.getBrandId().toString());
            }
            navVo.setNavValue(buffer.toString());
            navVo.setLink("http://search.gulimall.com/list.html?" + replace);
            navs.add(navVo);
            searchResult.setNavs(navs);
        }
        //属性相关
        if(!CollectionUtils.isEmpty(param.getAttrs())){
            List<SearchResult.NavVo> navs = searchResult.getNavs();
            List<SearchResult.NavVo> navVos = param.getAttrs().stream().map(attr -> {
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                String[] s = attr.split("_");
                //获取属性值
                navVo.setNavValue(s[1]);
                searchResult.getAttrIds().add(Long.parseLong(s[0]));
                //获取属性名
                String navName = searchResult.getAttrs().stream()
                        .filter(attrVo -> attrVo.getAttrId() == Long.parseLong(s[0]))
                        .map(SearchResult.AttrVo::getAttrName).findFirst().get();
                navVo.setNavName(navName);
                //获取面包屑路由
                String replace = replaceQueryString(param.getQueryString(), "attrs", attr);
                navVo.setLink("http://search.gulimall.com/list.html?" + replace);
                return navVo;
            }).collect(Collectors.toList());
            navs.addAll(navVos);
            searchResult.setNavs(navs);
        }
        return searchResult;
    }

    //url替换
    private String replaceQueryString(String queryString, String key, String value){
        //url请求地址对路径进行了编码，需要进行相同的编码才能成功匹配
        String encode = null;
        try {
            encode = URLEncoder.encode(value,"UTF-8");
            //浏览器对空格的编码和Java不一样，差异化处理
            encode.replace("+","%20");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        //将url请求地址里面的当前属性置空
        return queryString.replace("&" + key + "=" + encode, "").replace(key + "=" + encode + "&", "").replace(key + "=" + encode, "");

    }
}
