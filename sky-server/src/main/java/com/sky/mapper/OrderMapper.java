package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {

    /**
     * 将订单信息插入到order表中
     * @param orders
     */
    void insert(Orders orders);


    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 按订单id查询订单
     * @param orders
     * @return
     */
    @Select("select * from orders where id=#{id}")
    Orders getById(Orders orders);

    Page<Orders> pageQuery(OrdersPageQueryDTO dto);

    /**
     * 根据订单状态分组查询订单数量
     * @return
     */
    @Select("select count(*) from orders where status=#{status}")
    Integer getCountByStatus(Integer status);

    /**
     * 按照订单状态和订单下单时间查询订单
     * @param pendingPayment
     * @param flagTime
     * @return
     */
    @Select("select * from orders where status=#{pendingPayment} and order_time<#{flagTime}")
    List<Orders> getByStatusAndOrderTime( Integer pendingPayment, @Param("flagTime") LocalDateTime flagTime);

    /**
     * 统计营业额
     * @param map
     * @return
     */
    Double sumAmountByMap(Map map);

    // 根据时间段和订单状态统计订单个数
    Integer getOrdersStatisticsByMap(Map map);

    /**
     * 查询销量前10的菜品
     * @param beginTime
     * @param endTime
     * @return
     */
    List<GoodsSalesDTO> getTop10(LocalDateTime beginTime, LocalDateTime endTime);
}
