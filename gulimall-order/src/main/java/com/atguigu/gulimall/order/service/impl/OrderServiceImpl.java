package com.atguigu.gulimall.order.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.nacos.common.utils.ConvertUtils;
import com.atguigu.common.constant.OrderConstant;
import com.atguigu.common.to.*;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.order.entity.OrderItemEntity;
import com.atguigu.gulimall.order.entity.PaymentInfoEntity;
import com.atguigu.gulimall.order.exception.OrderException;
import com.atguigu.gulimall.order.feign.*;
import com.atguigu.gulimall.order.interceptor.OrderInterceptor;
import com.atguigu.gulimall.order.service.OrderItemService;
import com.atguigu.gulimall.order.service.PaymentInfoService;
import com.atguigu.gulimall.order.vo.*;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.order.dao.OrderDao;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.RequestContextHolder;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {
    private ThreadLocal<OrderSubmitVo> submitThreadLocal = new ThreadLocal<>();

    @Autowired
    private MemberFeignService memberFeignService;
    @Autowired
    private CartFeignService cartFeignService;
    @Autowired
    private ThreadPoolExecutor executor;
    @Autowired
    private WareFeignService wareFeignService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ProductFeignService productFeignService;
    @Autowired
    private OrderItemService orderItemService;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private ThirdPartFeignService thirdPartFeignService;
    @Autowired
    private PaymentInfoService paymentInfoService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );
        return new PageUtils(page);
    }

    //返回订单确认页所需数据
    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo orderConfirmVo = new OrderConfirmVo();
        MemberRespTo loginUser = OrderInterceptor.loginUser.get();
        RequestContextHolder.setRequestAttributes(RequestContextHolder.getRequestAttributes(), true);
        //远程查询所有地址信息
        CompletableFuture<Void> addressFuture = CompletableFuture.runAsync(() -> {
            List<MemberAddressVo> address = memberFeignService.getAddress(loginUser.getId());
            orderConfirmVo.setAddress(address);
        }, executor);
        //远程查询所有购物项信息
        CompletableFuture<Void> itemsFuture = CompletableFuture.runAsync(() -> {
            List<OrderItemVo> items = cartFeignService.getUserCartItems();
            orderConfirmVo.setItems(items);
        }, executor).thenRunAsync(() -> {
            List<OrderItemVo> items = orderConfirmVo.getItems();
            //远程查询商品库存信息
            List<Long> skuIds = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
            R hasStock = wareFeignService.getSkusHasStock(skuIds);
            List<SkuHasStockTo> skuHasStockTos = hasStock.getData("data", new TypeReference<List<SkuHasStockTo>>(){});
            if(!CollectionUtils.isEmpty(skuHasStockTos)){
                //将skuHasStockTos集合转换为map
                Map<Long, Boolean> skuHasStockMap = skuHasStockTos.stream()
                        .collect(Collectors.toMap(SkuHasStockTo::getSkuId, SkuHasStockTo::isHasStock));
                orderConfirmVo.setStocks(skuHasStockMap);
            }
        });
        CompletableFuture.allOf(addressFuture, itemsFuture).get();
        //获取积分信息
        orderConfirmVo.setIntegration(loginUser.getIntegration());
        //防重令牌
        String token = UUID.randomUUID().toString().replace("-", "");
        //同一个用户的key相同，token有可能被覆盖。不过没有关系，因为这是跳转结算页的方法，token值会被返回给前端
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX + loginUser.getId(), token, 30, TimeUnit.MINUTES);
        orderConfirmVo.setOrderToken(token);
        return orderConfirmVo;
    }

    //提交订单
    @Override
    @Transactional
    //@GlobalTransactional 不适用于高并发场景
    public OrderEntity submitOrder(OrderSubmitVo vo) {
        submitThreadLocal.set(vo);
        //验证令牌，使用lua脚本保证对比、删除的令牌原子性
        MemberRespTo loginUser = OrderInterceptor.loginUser.get();
        String key = OrderConstant.USER_ORDER_TOKEN_PREFIX + loginUser.getId();
        String orderToken = vo.getOrderToken();
        //当存储在键(KEYS[1])中的值与传入的参数值(ARGV[1])相等时，删除这个键，并返回删除操作的结果；否则返回0(在CategoryServiceImpl中也使用过)
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Long result = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList(key), orderToken);
        if(result == 0){ //令牌验证失败
            throw new OrderException("下单失败，令牌订单信息过期，请刷新再次提交");
        }
        //令牌验证成功，创建订单
        OrderCreateVo order = createOrder();
        //验证价格
        BigDecimal payAmount = order.getOrder().getPayAmount();
        BigDecimal payPrice = vo.getPayPrice();
        //payAmount.equals(payPrice)需要连精度也相同
        if(Math.abs(payAmount.subtract(payPrice).doubleValue()) >= 0.01){ //验价失败
            throw new OrderException("下单失败，订单商品价格发生变化，请确认后再次提交");
        }
        //验价成功，保存订单
        saveOrder(order);
        //锁定库存
        WareSkuLockTo to = new WareSkuLockTo();
        to.setOrderSn(order.getOrder().getOrderSn());
        //所有需要锁定的订单项信息
        List<OrderItemTo> orderItemTos = order.getOrderItems().stream().map(item -> {
            OrderItemTo orderItemTo = new OrderItemTo();
            orderItemTo.setSkuId(item.getSkuId());
            orderItemTo.setSkuName(item.getSkuName());
            orderItemTo.setCount(item.getSkuQuantity());
            return orderItemTo;
        }).collect(Collectors.toList());
        to.setLocks(orderItemTos);
        R r = wareFeignService.orderLockStock(to);
        if(r.getCode() != 0){ //锁定失败
            throw new OrderException("下单失败，商品id：" + r.get("msg") + "库存不足！");
        }
        //锁定成功，将订单信息放入延时队列
        rabbitTemplate.convertAndSend(OrderConstant.ORDER_EXCHANGE, OrderConstant.ORDER_DELAY_QUEUE_KEY, order.getOrder());
        submitThreadLocal.remove();
        return order.getOrder();
    }

    //创建订单
    public OrderCreateVo createOrder(){
        OrderCreateVo order = new OrderCreateVo();
        //生成订单号
        String orderSn = IdWorker.getTimeId();
        //获取订单
        OrderEntity orderEntity = buildOrder(orderSn);
        order.setOrder(orderEntity);
        //获取全部订单项
        List<OrderItemEntity> orderItemEntities = buildOrderItems(orderSn);
        order.setOrderItems(orderItemEntities);
        //价格、优惠计算
        computePrice(orderEntity, orderItemEntities);
        return order;
    }

    //构建订单数据
    private OrderEntity buildOrder(String orderSn) {
        OrderEntity orderEntity = new OrderEntity();
        //当前用户登录信息
        MemberRespTo loginUser = OrderInterceptor.loginUser.get();
        orderEntity.setMemberId(loginUser.getId());
        orderEntity.setOrderSn(orderSn);
        orderEntity.setMemberUsername(loginUser.getUsername());
        //运费
        OrderSubmitVo vo = submitThreadLocal.get();
        orderEntity.setFreightAmount(vo.getFarePrice());
        //收货人信息
        //根据id而不是memberId获取的地址，因此只会有一个
        MemberAddressVo address = memberFeignService.getAddressById(vo.getAddrId());
        BeanUtils.copyProperties(address, orderEntity, "id", "memberId", "areacode", "defaultStatus");
        //订单相关状态信息
        orderEntity.setPayType(vo.getPayType());
        orderEntity.setSourceType(0);
        orderEntity.setStatus(OrderConstant.OrderStatusEnum.CREATE_NEW.getCode());
        orderEntity.setDeliveryCompany("京东");
        orderEntity.setAutoConfirmDay(7);
        orderEntity.setNote(vo.getNote());
        orderEntity.setConfirmStatus(0);
        orderEntity.setDeleteStatus(0);
        return orderEntity;
    }

    //构建全部订单项数据
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        List<OrderItemEntity> orderItemEntityList = new ArrayList<>();
        //currentCartItems为选中的购物项
        List<OrderItemVo> currentCartItems = cartFeignService.getUserCartItems();
        if(!CollectionUtils.isEmpty(currentCartItems)){
            orderItemEntityList = currentCartItems.stream().map(item -> {
                //构建订单项数据
                OrderItemEntity orderItemEntity = buildOrderItem(item);
                orderItemEntity.setOrderSn(orderSn);
                return orderItemEntity;
            }).collect(Collectors.toList());
        }
        return orderItemEntityList;
    }

    //构建订单项数据
    private OrderItemEntity buildOrderItem(OrderItemVo item) {
        OrderItemEntity orderItemEntity = new OrderItemEntity();
        //spu信息
        Long skuId = item.getSkuId();
        R r = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoTo spuInfo = r.getData("spuInfo", new TypeReference<SpuInfoTo>(){});
        BeanUtils.copyProperties(spuInfo, orderItemEntity);
        orderItemEntity.setCategoryId(spuInfo.getCatelogId());
        //sku信息
        orderItemEntity.setSkuId(skuId);
        orderItemEntity.setSkuName(item.getTitle());
        orderItemEntity.setSkuPic(item.getImage());
        orderItemEntity.setSkuPrice(item.getPrice());
        orderItemEntity.setSkuQuantity(item.getCount());
        orderItemEntity.setSkuAttrsVals(String.join(";", item.getSkuAttrValues()));
        //优惠信息
        //订单项价格信息
        orderItemEntity.setPromotionAmount(BigDecimal.ZERO);
        orderItemEntity.setCouponAmount(BigDecimal.ZERO);
        MemberRespTo loginUser = OrderInterceptor.loginUser.get();
        BigDecimal integration = loginUser.getIntegration() == null ? BigDecimal.ZERO : new BigDecimal(loginUser.getIntegration());
        orderItemEntity.setIntegrationAmount(integration);
        //商品的积分信息
        //总额
        BigDecimal origin = item.getPrice().multiply(new BigDecimal(item.getCount()));
        //直接整型相乘可能导致精度值缺失
        orderItemEntity.setGiftGrowth(origin.intValue());
        orderItemEntity.setGiftIntegration(origin.intValue());
        //订单项的实际金额=总额-优惠价格
        BigDecimal subtract = origin.subtract(orderItemEntity.getCouponAmount())
                .subtract(orderItemEntity.getPromotionAmount())
                .subtract(orderItemEntity.getIntegrationAmount());
        orderItemEntity.setRealAmount(subtract);
        return orderItemEntity;
    }

    //价格、优惠计算
    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> orderItemEntities) {
        //总价
        BigDecimal total = BigDecimal.ZERO;
        //优惠价
        BigDecimal promotion = BigDecimal.ZERO;
        BigDecimal coupon = BigDecimal.ZERO;
        BigDecimal integration = BigDecimal.ZERO;
        //积分
        Integer integrationTotal = 0;
        //成长值
        Integer growthTotal = 0;
        //订单项累加
        for(OrderItemEntity orderItem : orderItemEntities){
            promotion = promotion.add(orderItem.getPromotionAmount());
            coupon = coupon.add(orderItem.getCouponAmount());
            integration = integration.add(orderItem.getIntegrationAmount());
            total = total.add(orderItem.getRealAmount());
            integrationTotal += orderItem.getGiftIntegration();
            growthTotal += orderItem.getGiftGrowth();
        }
        //获取各价格数据
        orderEntity.setTotalAmount(total);
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));
        orderEntity.setPromotionAmount(promotion);
        orderEntity.setCouponAmount(coupon);
        orderEntity.setIntegrationAmount(integration);
        orderEntity.setIntegration(integrationTotal);
        orderEntity.setGrowth(growthTotal);
    }

    //保存订单
    private void saveOrder(OrderCreateVo vo){
        //保存订单
        OrderEntity order = vo.getOrder();
        this.baseMapper.insert(order);
        //批量保存订单项(只有service有批量保存方法)
        List<OrderItemEntity> orderItems = vo.getOrderItems();
        orderItems = orderItems.stream().map(item -> {
            item.setOrderId(order.getId());
            return item;
        }).collect(Collectors.toList());
        orderItemService.saveBatch(orderItems);
    }

    //根据订单编号查询订单状态
    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        return this.baseMapper.selectOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
    }

    //定时关闭订单
    @Override
    public void closeOrder(OrderEntity order) {
        //查询订单状态
        OrderEntity orderEntity = this.getById(order.getId());
        //如果状态还是待支付，则关单
        if(orderEntity.getStatus() == OrderConstant.OrderStatusEnum.CREATE_NEW.getCode()){
            orderEntity.setStatus(OrderConstant.OrderStatusEnum.CANCLED.getCode());
            this.baseMapper.updateById(orderEntity);
            //解锁库存
            OrderTo to = new OrderTo();
            BeanUtils.copyProperties(orderEntity, to);
            //消息可能发送失败，最好将发送的消息在数据库做好记录，并定期重发失败消息
            rabbitTemplate.convertAndSend(OrderConstant.ORDER_EXCHANGE, OrderConstant.ORDER_RELEASE_QUEUE_KEY, to);
        }
    }

    //支付宝支付
    @Override
    public String getOrderPay(String orderSn) {
        PayTo to = new PayTo();
        OrderEntity orderEntity = this.getOrderByOrderSn(orderSn);
        String pay = null;
        if(orderEntity.getStatus() == OrderConstant.OrderStatusEnum.CREATE_NEW.getCode()){
            to.setOutTradeNo(orderSn);
            //保留两位小数点，向上取值
            BigDecimal payAmount = orderEntity.getPayAmount().setScale(2, BigDecimal.ROUND_UP);
            to.setTotalAmount(payAmount.toString());
            //查询订单项数据
            List<OrderItemEntity> orderItems = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn));
            to.setSubject(orderItems.get(0).getSpuName());
            JSONArray goodsDetails = new JSONArray();
            orderItems.forEach(item -> {
                JSONObject bizContent = new JSONObject();
                bizContent.put("goods_id", item.getSkuId());
                bizContent.put("goods_name", item.getSkuName());
                bizContent.put("quantity", item.getSkuQuantity());
                bizContent.put("price", item.getSkuPrice());
                goodsDetails.add(bizContent);
            });
            to.setGoodsDetail(goodsDetails);
            pay = thirdPartFeignService.pay(to);
        }
        return pay;
    }

    //分页获取订单具体数据
    @Override
    public PageUtils queryPageWithItems(Map<String, Object> params) {
        MemberRespTo loginUser = OrderInterceptor.loginUser.get();
        //查询用户订单
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>().eq("member_id", loginUser.getId()).orderByDesc("create_time")
        );
        //查询用户订单中的订单项
        page.getRecords().stream().forEach(order -> {
            List<OrderItemEntity> itemEntities = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", order.getOrderSn()));
            order.setOrderItems(itemEntities);
        });
        return new PageUtils(page);
    }

    //处理支付结果
    @Override
    @Transactional
    public void handlePayResult(PayAsyncVo vo) {
        //保存交易流水
        PaymentInfoEntity paymentInfo = new PaymentInfoEntity();
        paymentInfo.setOrderSn(vo.getOut_trade_no());
        paymentInfo.setAlipayTradeNo(vo.getTrade_no());
        paymentInfo.setTotalAmount(new BigDecimal(vo.getBuyer_pay_amount()));
        paymentInfo.setSubject(vo.getBody());
        paymentInfo.setPaymentStatus(vo.getTrade_status());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        paymentInfo.setCreateTime(LocalDateTime.parse(vo.getGmt_create(), formatter));
        paymentInfo.setConfirmTime(LocalDateTime.parse(vo.getGmt_payment(), formatter));
        paymentInfo.setCallbackTime(LocalDateTime.parse(vo.getNotify_time(), formatter));
        paymentInfoService.save(paymentInfo);
        //获取当前状态，交易通知状态为TRADE_SUCCESS或TRADE_FINISHED时，支付宝才会认定为买家付款成功
        String status = vo.getTrade_status();
        if("TRADE_SUCCESS".equals(status) || "TRADE_FINISHED".equals(status)){
            //修改订单状态
            this.baseMapper.updateOrderStatus(vo.getOut_trade_no(), OrderConstant.OrderStatusEnum.PAYED.getCode());
            //因为还没有发货，库存没有改变，不需要在这个时候修改库存信息
        }
    }
}