package com.sky.controller.admin;

import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("adminOrderController")
@Api(tags = "管理端订单接口")
@RequestMapping("/admin/order")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 条件查询订单
     *
     * @param dto
     * @return
     */
    @GetMapping("/conditionSearch")
    public Result<PageResult> conditionSearch(OrdersPageQueryDTO dto) {
        PageResult pageResult = orderService.conditionSearch(dto);
        return Result.success(pageResult);
    }

    /**
     * 各个状态的订单数量统计
     *
     * @return
     */
    @GetMapping("/statistics")
    @ApiOperation("查询各个状态订单数量")
    public Result<OrderStatisticsVO> statistics() {
        OrderStatisticsVO vo = orderService.getCountByStatus();
        return Result.success(vo);
    }

    /**
     * 根据订单id查询订单详情
     *
     * @param id
     * @return
     */
    @GetMapping("/details/{id}")
    @ApiOperation("管理端查询订单详情")
    public Result<OrderVO> getDetailsById(@PathVariable Long id) {
        OrdersDTO ordersDTO = new OrdersDTO();
        ordersDTO.setId(id);
        OrderVO orderVO = orderService.getDetaliById(ordersDTO);
        return Result.success(orderVO);
    }

    /**
     * 管理端接单
     * 根据订单id修改订单状态
     * @return
     */
    @PutMapping("/confirm")
    @ApiOperation("接单")
    public Result confirm(@RequestBody OrdersConfirmDTO dto){
        orderService.confirm(dto);
        return Result.success();
    }


    /**
     * 管理端拒单
     * @param dto
     * @return
     */
    @PutMapping("/rejection")
    @ApiOperation("拒单")
    public Result rejection(@RequestBody OrdersRejectionDTO dto){
        orderService.rejection(dto);
        return Result.success();
    }

    /**
     * 管理端取消订单
     * @param dto
     * @return
     */
    @PutMapping("/cancel")
    @ApiOperation("取消订单")
    public Result cancel(@RequestBody OrdersCancelDTO dto){
        orderService.cancelOrderAdmin(dto);
        return Result.success();
    }

    /**
     * 派送订单
     * @param id
     * @return
     */
    @PutMapping("/delivery/{id}")
    @ApiOperation("派送订单")
    public Result delivery(@PathVariable Long id){
        orderService.delivery(id);
        return Result.success();
    }

    /**
     * 管理端完成订单
     * @param id
     * @return
     */
    @PutMapping("/complete/{id}")
    @ApiOperation("完成订单")
    public Result complete(@PathVariable Long id){
        orderService.complete(id);
        return Result.success();
    }
}
