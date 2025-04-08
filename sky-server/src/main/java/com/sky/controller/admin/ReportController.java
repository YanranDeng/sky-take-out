package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.time.LocalDate;

@RestController
@Api(tags = "营业统计数据接口")
@RequestMapping("/admin/report")
@Slf4j
public class ReportController {

    @Autowired
    private ReportService reportService;

    /**
     * 统计特定时间段的营业额,传入开始时间和结束时间,
     * 返回统计日期和对应的营业额,两个数据都是以字符串形式返回
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/turnoverStatistics")
    @ApiOperation("营业额统计接口")
    public Result<TurnoverReportVO> turnoverStatistics(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
                                                       @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        TurnoverReportVO turnoverReportVO = reportService.turnoverStatistics(begin,end);
        return Result.success(turnoverReportVO);
    }


    /**
     * 用户数量统计,
     * 包含每日新增数量以及每日实时数量
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/userStatistics")
    @ApiOperation("用户统计接口")
    public Result<UserReportVO> userStatistics(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
                                               @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){
        UserReportVO userReportVO = reportService.userStatistics(begin,end);
        return Result.success(userReportVO);
    }

    /**
     * 订单统计接口
     * 用于统计指定时间段的总订单和有效订单数,以及单日的订单数
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/ordersStatistics")
    @ApiOperation("订单统计接口")
    public Result<OrderReportVO> ordersStatistics(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
                                                  @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){
        OrderReportVO orderReportVO = reportService.getOrdersStatistics(begin,end);
        return Result.success(orderReportVO);
    }


    /**
     * 统计销量排名前10名的菜品或套餐
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/top10")
    @ApiOperation("销量前10统计接口")
    public Result<SalesTop10ReportVO> getTop10(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
                                               @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){
        SalesTop10ReportVO vo = reportService.getTop10(begin,end);
        return Result.success(vo);
    }


    /**
     * 导出Excel报表数据
     * 查询对应数据并写入excel表中,而后返回给客户端浏览器
     * @param response
     */
    @GetMapping("/export")
    @ApiOperation("导出Excel报表接口")
    public void exportExcel(HttpServletResponse response) throws FileNotFoundException {
        reportService.exportExcel(response);
    }
}

