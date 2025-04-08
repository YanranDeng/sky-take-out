package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.properties.ShopProperties;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderdetailMapper orderdetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Autowired
    private ShopProperties shopProperties;
    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 添加订单信息
     *
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    @Transactional
    public OrderSubmitVO order(OrdersSubmitDTO ordersSubmitDTO) {

        // 1,异常排除,判断传入的OrdersDTO对象中的地址和购物车数据是否为空,若为空直接抛出异常由全局异常处理器处理
        ShoppingCart shoppingCart = new ShoppingCart();
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);


        // 查询当前用户的购物车是否为空
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList == null || shoppingCartList.isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 查询前端dto传入的地址是否为空
        AddressBook addr = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addr == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        // 调用checkOutOfRange方法校验订单距离是否合法,如合法则不会抛出异常点单继续
        // 拼接用户地址
        String address = addr.getCityName()+addr.getDistrictName()+addr.getDetail();
        // 调用checkOutOfRange方法校验
        checkOutOfRange(address);


        // 2,向orders表中插入订单数据,并返回订单的主键id
        Orders orders = new Orders();
        // 填充orders的属性值
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setUserId(userId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setConsignee(addr.getConsignee());
        orders.setPhone(addr.getPhone());
        orders.setAddress(addr.getDetail());


        // 将订单数据插入到orders表中,并将订单id返回赋值给orders对象(该步骤在xml文件的insert方法中定义)
        orderMapper.insert(orders);


        // 3,向orders_detail表中插入购物车菜品订单数据

        // 创建一个list集合用于存放订单补充数据对象orderDetail,该对象封装了该用户本次订单中商品的具体数据(如商品的id,价格,口味等)
        List<OrderDetail> orderDetailList = new ArrayList<>();
        // 创建OrderDetail对象用于封装用户订单商品信息
        OrderDetail orderDetail = new OrderDetail();
        // 遍历用户购物车,用于取出用户的点餐商品数据,便于往OrderDetail对象中填充点餐商品信息
        for (ShoppingCart cart : shoppingCartList) {
            // 为OrderDetail填充字段属性
            OrderDetail detail = orderDetail.builder()
                    .orderId(orders.getId())
                    .dishId(cart.getDishId())
                    .name(cart.getName())
                    .setmealId(cart.getSetmealId())
                    .amount(cart.getAmount())
                    .number(cart.getNumber())
                    .dishFlavor(cart.getDishFlavor())
                    .image(cart.getImage())
                    .build();
            // 将每一个OrderDetail对象都放到list集合中
            orderDetailList.add(detail);
        }
        // 将具体的点餐商品数据OrderDetail批量插入order_detail表中,这里通过OrderDetail列表的形式批量插入
        orderdetailMapper.insertBatch(orderDetailList);


        // 4,待订单数据添加后,将当前用户的购物车数据删除
        shoppingCartMapper.deleteByUserId(userId);

        // 5,返回订单信息
        // 从订单对象orders对象中取出相应字段封装成OrderSubmitVO对象并返回给前端
        OrderSubmitVO orderSubmitVO = new OrderSubmitVO();
        orderSubmitVO.setOrderNumber(orders.getNumber());
        orderSubmitVO.setOrderAmount(orders.getAmount());
        orderSubmitVO.setOrderTime(orders.getOrderTime());
        orderSubmitVO.setId(orders.getId());
        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        // 模拟微信支付,直接跳过预支付交易单生成
/*        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );*/
        // 生成空的JSONObject对象,使代码继续运行.
        JSONObject jsonObject = new JSONObject();

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        // 当订单支付成功并且订单数据被更新后需要调用webSocketServer对象来对管理端浏览器进行来单提醒
        // 提醒信息由三部分构成:提醒状态码,提醒订单号,提醒信息体
        Map map = new HashMap();
        map.put("type",1); // 与前端约定,type为1表示来单提醒,type为2表示用户催单
        map.put("orderId",ordersDB.getId()); // 写入订单id
        map.put("content","订单号:"+ordersDB.getNumber()); // 写入订单号
        // 将map集合转为json格式并使用webSocketServer对象调用sendToAllClient方法向浏览器发送信息
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
        log.info("已通过webSocketServer向浏览器发送来单提醒");
    }

    /**
     * 根据订单id查询订单详情
     *
     * @param dto
     * @return
     */
    @Override
    public OrderVO getDetaliById(OrdersDTO dto) {
        Orders orders = new Orders();
        BeanUtils.copyProperties(dto, orders);

        // 根据订单id查询orders表,得到订单信息封装到orders对象中.
        Orders ordersBase = orderMapper.getById(orders);

        // 根据订单id查询orders_detail表,将查询的订单详细数据封装到List集合中,集合的泛型是OrderDetail.
        List<OrderDetail> list = orderdetailMapper.getByOrderId(orders);

        // 将以上两个查询的数据封装到OrdersVO对象中并返回
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(ordersBase, orderVO);
        orderVO.setOrderDetailList(list);
        return orderVO;
    }

    /**
     * 查询历史订单
     *
     * @param dto
     * @return
     */
    @Override
    public PageResult getHistory(OrdersPageQueryDTO dto) {
        // 获取当前微信用户的用户id
        dto.setUserId(BaseContext.getCurrentId());
        // 设置分页参数
        PageHelper.startPage(dto.getPage(), dto.getPageSize());
        // 开始分页,并返回分页查询的结果,page对象中封装的是分页查询记录数和分页查询的对象集合
        Page<Orders> page = orderMapper.pageQuery(dto);

        // 新建List集合用于存放填充后的ordervo对象
        List<OrderVO> orderVOList = new ArrayList<>();
        // 排除异常,判断page对象是否为空
        if (page != null && page.getTotal() > 0) {
            // 遍历page对象,将集合中的orders对象依次取出
            for (Orders orders : page) {
                // 使用orders对象的主键id查询与之相关联的订单详细表的数据并返回封装到list集合中
                List<OrderDetail> orderDetailList = orderdetailMapper.getByOrderId(orders);
                // 将orders对象和与之对应的ordersdetail集合对象再次封装成ordervo对象.
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetailList);
                // 将封装好的orders对象放入新建的list集合中,以便于最后封装pageresult对象.
                orderVOList.add(orderVO);
            }
        }

        return new PageResult(page.getTotal(), orderVOList);
    }

    /**
     * 取消订单
     *
     * @param id
     */
    @Override
    public void cancelOrder(Long id) {
        Orders orders = new Orders();
        orders.setId(id);
        // 1,使用订单id查询订单表,判断该订单是否存在
        Orders order = orderMapper.getById(orders);
        // 若订单不存在,则直接抛出异常由全局异常处理器处理
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 2,若订单存在,则判断该订单的状态是否为未接单,未支付.若订单状态为这两个状态可以直接取消,
        // 若不为这两个状态,已经进入下一个状态(已接单,派送中等)则不可直接取消,需要用户和商家协商.
        // 这里为了简便处理,若状态不为可以直接取消的状态,则直接抛出异常
        //      订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if (order.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 3,判断订单状态是否已经支付,若已经支付需要将费用退回,
        // 正常情况下这里需要先调用WeChatPayUtil中的refund方法请求微信退款接口,待请求结束后才能修改数据表的支付状态
        // 但是由于微信小程序对于个人账户的限制,个人小程序无法开通支付/退款功能,因此这里只能模拟退款.
        // 具体方法是判断订单状态是否为2(待接单),若为2则直接修改订单的支付状态为已退款.
//              支付状态 0未支付 1已支付 2退款
        if (Orders.TO_BE_CONFIRMED.equals(order.getStatus())) {
            // 为了执行效率,先将支付状态存储在order对象中,最后统一执行更新操作
            orders.setPayStatus(Orders.REFUND);
        }

        // 4,排除以上异常和检查支付状态后,这里对order对象进行公共字段填充,包括取消时间,取消原因,和修改订单状态为取消
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        // 更新订单数据
        orderMapper.update(orders);
    }

    /**
     * 再来一单
     * 主要业务逻辑就是通过传入订单id查询该订单的订单明细表,并将明细表中的菜品或套餐重新放入该用户的购物车中
     *
     * @param id
     */
    @Override
    public void repetition(Long id) {
        // 获取当前用户id
        Long userId = BaseContext.getCurrentId();

        Orders orders = new Orders();
        orders.setId(id);
        // 通过订单id查询订单明细表,获取该订单中的套餐或菜品
        List<OrderDetail> orderDetailList = orderdetailMapper.getByOrderId(orders);

        // 将订单明细对象OrderDetail对象转化为购物车对象并放到list集合中
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(new Function<OrderDetail, ShoppingCart>() {
            @Override
            public ShoppingCart apply(OrderDetail orderDetail) {
                ShoppingCart shoppingCart = new ShoppingCart();
                // 复制属性,将主键id排除
                BeanUtils.copyProperties(orderDetail, shoppingCart, "id");
                shoppingCart.setUserId(userId);
                shoppingCart.setCreateTime(LocalDateTime.now());
                return shoppingCart;
            }
        }).collect(Collectors.toList());

        // 向购物车表中批量添加购物车数据
        shoppingCartMapper.saveBatch(shoppingCartList);
    }

    /**
     * 管理端条件查询订单
     *
     * @param dto
     * @return
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO dto) {

        // 设置分页参数
        PageHelper.startPage(dto.getPage(), dto.getPageSize());
        // 查询orders表
        Page<Orders> page = orderMapper.pageQuery(dto);

        // 调用getOrdersVO方法,传入分页结果page,解析为orderVO集合,
        // 该集合中封装着查询到的order对象,order对象中封装着订单信息
        List<OrderVO> orderVOS = getOrdersVO(page);

        // 返回pageResult对象,封装两个属性,一个是查询数量,一个是orderVO集合.
        return new PageResult(page.getTotal(), orderVOS);
    }

    /**
     * 各个状态的订单数量统计
     *
     * @return
     */
    @Override
    public OrderStatisticsVO getCountByStatus() {
        // 根据状态查询订单数量
        Integer toBeConfirmed = orderMapper.getCountByStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.getCountByStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.getCountByStatus(Orders.DELIVERY_IN_PROGRESS);

        // 新建OrderStatisticsVO对象并将上述查询结果封装并返回
        return new OrderStatisticsVO(toBeConfirmed, confirmed, deliveryInProgress);
    }

    /**
     * 管理端接单
     *
     * @param dto
     */
    @Override
    public void confirm(OrdersConfirmDTO dto) {
        Orders orders = new Orders();
        orders.setId(dto.getId());
        orders.setStatus(Orders.CONFIRMED);
        orderMapper.update(orders);
    }

    /**
     * 管理端拒单
     *
     * @param dto
     */
    @Override
    @Transactional
    public void rejection(OrdersRejectionDTO dto) {
        log.info("拒单数据为:{}", dto);
        Orders orders = new Orders();
        BeanUtils.copyProperties(dto, orders);
        // 1,根据订单id查询订单,判断是否存在
        Orders orderTemp = orderMapper.getById(orders);
        if (orderTemp == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 2,若存在则判断该订单的状态,若状态为未接单的状态才可拒单,若不为未接单则直接抛出异常
        if (!Orders.TO_BE_CONFIRMED.equals(orderTemp.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 3,若订单存在且处于未接单的状态,则查询其支付状态,若支付状态为已支付,则需要执行退款操作
        if (orderTemp.getPayStatus().equals(Orders.PAID)) {
            // 模拟退款,直接修改orders对象的支付状态属性为已退款,后续数据表也会根据该对象属性更新字段信息
            orders.setPayStatus(Orders.REFUND);
            log.info("已退款给客户");
        }
        // 4,退款后执行更新订单信息操作,将支付状态(若修改),拒绝原因,拒绝时间,订单状态修改
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelTime(LocalDateTime.now());

        orderMapper.update(orders);

    }

    /**
     * 管理端取消订单
     *
     * @param dto
     */
    @Override
    @Transactional
    public void cancelOrderAdmin(OrdersCancelDTO dto) {
        Orders orders = new Orders();
        BeanUtils.copyProperties(dto, orders);

        // 1,根据订单id查询订单,判断是否存在
        Orders order = orderMapper.getById(orders);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 2,判断订单的状态,若订单状态为已支付,取消订单需要退款,并更改订单的支付状态为已退款
        if (order.getPayStatus() == 1) {
            orders.setPayStatus(2);
            log.info("订单即将取消,已经退款");
        }

        // 3,填充取消相关的字段并更新订单数据
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelTime(LocalDateTime.now());

        orderMapper.update(orders);

    }

    /**
     * 派送订单
     *
     * @param id
     */
    @Override
    public void delivery(Long id) {
        Orders orders = new Orders();
        orders.setId(id);
        // 1,根据id查询订单,判断是否为空
        Orders orderTemp = orderMapper.getById(orders);
        if (orderTemp == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 2,判断查询订单的状态是否为"已接单"(只有接单后才能进行订单派送),若不是接单状态则抛出异常
        if (!Orders.CONFIRMED.equals(orderTemp.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);

        }
        // 3,修改订单数据并更新数据表
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
        log.info("订单状态修改为配送中");
        orderMapper.update(orders);
    }

    /**
     * 完成订单
     *
     * @param id
     */
    @Override
    public void complete(Long id) {
        Orders orders = new Orders();
        orders.setId(id);
        // 1,查询并判断订单是否为空
        Orders orderTemp = orderMapper.getById(orders);
        if (orderTemp == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 2,判断订单的状态是否为"派送中",这是由于只有订单状态进行到"派送中"才能进入下一个状态"完成",
        // 若不为派送中则抛出异常.
        if (!Orders.DELIVERY_IN_PROGRESS.equals(orderTemp.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        // 3,修改订单状态并更新数据表
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());
        log.info("订单以完成");
        orderMapper.update(orders);

    }

    /**
     * 用户催单
     * @param id
     */
    @Override
    public void reminder(Long id) {
        // 根据id查询该订单
        Orders orders = new Orders();
        orders.setId(id);
        Orders orderTemp = orderMapper.getById(orders);
        // 判断订单是否存在
        if (orderTemp == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 若订单存在,则调用webSocketServer对象的sendToAllClient方法向管理端浏览器发送催单信息
        // 封装催单信息到Map集合并将集合转化为json对象字符串
        Map map = new HashMap();
        map.put("type",2); // 信息状态码,1为来单提醒,2为催单信息
        map.put("orderId",id); // 订单id
        map.put("content","订单号:"+orderTemp.getNumber()); // 提示信息,一般为订单号
        String json = JSON.toJSONString(map);
        // 利用webSocketServer对象的sendToAllClient方法将催单信息实时推送到浏览器上
        webSocketServer.sendToAllClient(json);

    }


    // 将orderDetailList中的菜品/套餐数据转化为字符串格式再封装到list中
    private List<String> toDetailString(List<OrderDetail> list) {
        List<String> orderDetail = list.stream().map(new Function<OrderDetail, String>() {
            @Override
            public String apply(OrderDetail orderDetail) {
                // 使用"商品名*商品价格"格式组成字符串
                String string = orderDetail.getName() + "*" + orderDetail.getNumber() + ";";
                return string;
            }
            // 将字符串封装到List<String>
        }).collect(Collectors.toList());

        return orderDetail;
    }

    /**
     * 传入分页结果,解析为ordersVO集合
     *
     * @param page
     * @return
     */
    private List<OrderVO> getOrdersVO(Page<Orders> page) {
        List<OrderVO> orderVOS = new ArrayList<>();
        // 判断page是否为空,若不为空则遍历page
        if (page != null) {
            for (Orders orders : page) {

                // 获取订单号
                Long id = orders.getId();
                Orders orderTemp = new Orders();
                orderTemp.setId(id);

                // 根据订单id查询订单明细并封装到List<OrderDetail>
                List<OrderDetail> orderDetailList = orderdetailMapper.getByOrderId(orderTemp);

                // 将orderDetailList中的菜品/套餐数据转化为字符串格式再封装到list中
                List<String> orderDetail = toDetailString(orderDetailList);

                // 使用String类的join方法将orderDetail集合中的字符串拼接在一起
                String orderDetailStr = String.join("", orderDetail);

                // 新建OrderVO对象,使用BeanUtils拷贝属性
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                // 将orderDetailStr字符串填充到vo对象中
                orderVO.setOrderDishes(orderDetailStr);

                // 添加vo对象到list集合中
                orderVOS.add(orderVO);
            }
        }
        return orderVOS;
    }

    private void checkOutOfRange(String address) {
        // 封装百度地图地理编码接口的请求数据(用户地址)
        HashMap<String, String> map = new HashMap<>();
        map.put("address", address);
        map.put("ak", shopProperties.getAk());
        map.put("output", "json");

        // 使用HttpClientUtil类的doGet方法,传入请求地址和请求参数集合来向目标地址发出请求,返回值是json字符串
        String userLocation = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3/", map);

        // 将返回的json字符串解析成json对象
        JSONObject jsonObject = JSONObject.parseObject(userLocation);

        // 检查响应数据的状态码,百度约定返回为0表示响应正常
        if (!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException("收货地址解析地址失败");
        }

        // 从json对象中回去用户地址的经纬度,并封装到jsonobject对象中
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        // 拼接用户地址的经纬度坐标,形式为"纬度,经度"
        String userLocationStr = location.getString("lat") + "," + location.getString("lng");


        // 与上述取用户地址的经纬度过程一致,店铺地址取经纬度过程如法炮制
        map.put("address", shopProperties.getAddress());
        String shopLocation = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3/", map);
        jsonObject = JSONObject.parseObject(shopLocation);
        if (!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException("店铺地址解析失败");
        }
        JSONObject location1 = jsonObject.getJSONObject("result").getJSONObject("location");
        String shopLocationStr = location1.getString("lat") + "," + location1.getString("lng");


        // 封装路径规划接口的请求参数
        map.put("origin", shopLocationStr);
        map.put("destination", userLocationStr);
        map.put("steps_info", "0");

        // 向路径规划接口发起请求
        String result = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", map);
        // 解析返回结果为json对象
        jsonObject = JSONObject.parseObject(result);

        // 判断返回结果的状态码是否正常(也即是否为0)
        if (!jsonObject.getString("status").equals("0")) {
            new OrderBusinessException("配送路径规划失败");
        }

        // 从json对象中取出配送路径长度属性所在的子JsonArray对象
        JSONArray jsonArray = (JSONArray) jsonObject.getJSONObject("result").get("routes");

        // 将上述得到的JSONArray对象的第一个索引的元素取出并转化为JSONObject对象,
        // 然后通过字段名回去该JSONObject对象的封装的distance属性,然后将其转化为Integer类型
        Integer distance = Integer.valueOf(((JSONObject) jsonArray.get(0)).getString("distance"));

        log.info("两地距离为:{}",distance);
        // 判断两地的距离是否大于8千米,若超过该阈值则表明配送距离过远,直接抛出异常提醒点单失败
        if (distance >= 8000) {
            throw new OrderBusinessException("超出配送范围");
        }

    }
}
