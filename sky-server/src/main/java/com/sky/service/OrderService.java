package com.sky.service;

import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService {
    /**
     * 添加订单信息
     *
     * @param ordersSubmitDTO
     * @return
     */
    OrderSubmitVO order(OrdersSubmitDTO ordersSubmitDTO);


    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    /**
     * 根据订单id查询订单详情
     *
     * @param dto
     * @return
     */
    OrderVO getDetaliById(OrdersDTO dto);

    PageResult getHistory(OrdersPageQueryDTO dto);

    void cancelOrder(Long id);


    /**
     * 再来一单
     *
     * @param id
     */
    void repetition(Long id);

    /**
     * 管理端条件查询订单
     *
     * @param dto
     * @return
     */
    PageResult conditionSearch(OrdersPageQueryDTO dto);

    /**
     * 各个状态的订单数量统计
     * @return
     */
    OrderStatisticsVO getCountByStatus();

    /**
     * 管理端接单
     * @param dto
     */
    void confirm(OrdersConfirmDTO dto);

    /**
     * 拒单
     * @param dto
     */
    void rejection(OrdersRejectionDTO dto);

    void cancelOrderAdmin(OrdersCancelDTO dto);

    /**
     * 派送订单
     * @param id
     */
    void delivery(Long id);

    /**
     * 完成订单
     * @param id
     */
    void complete(Long id);

    /**
     * 用户催单
     * @param id
     */
    void reminder(Long id);
}
