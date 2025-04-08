package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 营业额统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end) {
        // 1,根据传入的开始时间和结束时间来构造TurnoverReportVO的dateList
        // 新建一共list集合用来临时存放localdate日期对象
        List<LocalDate> localDates = new ArrayList<>();
        // 添加开始时间begin到集合中
        localDates.add(begin);
        // 使用while循环向list集合中添加localdate对象,
        // 终止条件是begin和end相同时(由于循环体中是添加当前begin的后一天,因此终止条件是begin和end相同而不是begin和end后一天相同)
        while (!begin.equals(end)) {
            // 条件满足时将当前begin对象增加一天,表示后续一天
            begin = begin.plusDays(1);
            // 添加begin到list中
            localDates.add(begin);
        }


        // 2,遍历时间集合localDates,使用每一个时间元素查询orders表,得到该天的营业额
        // 新建Map集合用于封装查询所需参数
        Map map = new HashMap();
        // 新建list集合用于存放查询到的单日营业额
        List<Double> amounts = new ArrayList<>();
        for (LocalDate localDate : localDates) {
            // 定义查询时间区间,每一天的查询时间都是从当天的00:00:00到23:59:59
            LocalDateTime min = LocalDateTime.of(localDate, LocalTime.MIN); // 00:00:00
            LocalDateTime max = LocalDateTime.of(localDate, LocalTime.MAX); // 23:59:59

            // 在查询Map中封装三个参数,分别是订单状态,以及起止时间,只有订单状态为5(已完成)才被划入统计范围
            map.put("status", Orders.COMPLETED);
            map.put("beginTime", min);
            map.put("endTime", max);

            // 调用OrderMapper的sumAmountByMap方法,传入map查询orders表,返回单日的营业额
            Double amount = orderMapper.sumAmountByMap(map);

            // 判断是否查询出来数据,若当日没有已完成的订单(没有营收),Mapper则会查询返回null,这时就要进行判断,
            // 若为null则手动将amount改为0,若不是null则维持原值.
            amount = amount == null ? 0 : amount;

            // 将单日的营业额按顺序放入list集合中
            amounts.add(amount);
        }

        // 3,转换并封装得到的两个查询数据集合,一个是日期集合,一个是营业额集合,将它们都转化为字符串并封装成OrderReportVO对象返回
        // 使用StringUtils类将localDates集合转化为字符串
        String dateList = StringUtils.join(localDates, ",");
        String amountList = StringUtils.join(amounts, ",");
        // 向TurnoverReportVO对象中封装数据并返回
        TurnoverReportVO turnoverReportVO = TurnoverReportVO.builder()
                .dateList(dateList)
                .turnoverList(amountList)
                .build();

        return turnoverReportVO;
    }

    /**
     * 用户数量统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO userStatistics(LocalDate begin, LocalDate end) {
        // 1,将begin到end之间的日期(包含begin和end)都放入一个集合中以备使用
        List<LocalDate> localDates = new ArrayList<>();
        localDates.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            localDates.add(begin);
        }

        // 2,遍历日期集合,使用每一个日期查询user表,得到当日新增用户以及当日累计用户
        // 构建两个集合,一个用于存放新增用户数据,一共用于存放累计用户
        List<Integer> newList = new ArrayList<>();
        List<Integer> totalList = new ArrayList<>();

        // 遍历日期集合
        for (LocalDate localDate : localDates) {
            // 使用日期对象构造当天的时间查询范围,也即该日的起始查询时刻与终止时刻
            LocalDateTime min = LocalDateTime.of(localDate, LocalTime.MIN);  //00:00:00
            LocalDateTime max = LocalDateTime.of(localDate, LocalTime.MAX);  //23:59:59

            // 将min和max两个查询参数封装到一共Map集合中用于后续查询
            Map map = new HashMap();
            // 先传入截至时间,用于查询累计用户数量
            map.put("endTime", max);
            // 调用userMapper层的countByMap方法,传入map集合查询累计用户数量
            Integer total = userMapper.countByMap(map);
            // 将得到的累计数量total放到totalList中
            totalList.add(total);

            // 在封装起始时间到map集合中,这时map集合中既有终止时间又有起始时间,两个时间就可以查询当天新增用户了
            map.put("beginTime", min);
            // 调用userMapper层的countByMap方法查询到该日期当天新增用户数量
            Integer sameDay = userMapper.countByMap(map);
            // 将当天新增数量放到newList中
            newList.add(sameDay);
        }

        // 3,将存有日期数据的集合localDates,存有累计用户数据的集合totalList,
        // 存有新增用户数据的集合newList转化为字符串并封装到UserReportVO对象中并返回.
        UserReportVO userReportVO = UserReportVO.builder()
                .dateList(StringUtils.join(localDates, ","))
                .newUserList(StringUtils.join(newList, ","))
                .totalUserList(StringUtils.join(totalList, ","))
                .build();
        return userReportVO;
    }

    /**
     * 订单统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {
        // 1,将begin到end之间的日期(包含begin和end)都放入一个集合中以备使用
        List<LocalDate> localDates = new ArrayList<>();
        localDates.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            localDates.add(begin);
        }

        // 2,遍历日期集合localDates,使用其中的日期来查询单日的总订单和有效订单
        // 新建两个集合用来存放单日总订单和有效订单数据
        List<Integer> totalList = new ArrayList<>();
        List<Integer> validList = new ArrayList<>();

        // 定义两个变量,用于记录传入时期(全时期)内的总订单数和有效订单数
        Integer totalSum = 0;
        Integer validSum = 0;

        // 遍历日期列表,使用其中日期查询orders表
        for (LocalDate localDate : localDates) {
            // 利用日期对象封装精确的查询开始终止时刻
            LocalDateTime beginTime = LocalDateTime.of(localDate, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(localDate, LocalTime.MAX);

            // 先查询单日的总订单数,新建参数Map,使用该Map传递查询参数
            Map map = new HashMap();
            map.put("beginTime", beginTime);
            map.put("endTime", endTime);
            // 调用orderMapper层中的getOrdersStatisticsByMap方法查询单日总订单数
            Integer total = orderMapper.getOrdersStatisticsByMap(map);
            // 将查询的单日中订单数累加到全时期总订单数上
            totalSum += total;
            // 将查询到的单日订单数添加到单日总订单集合中
            totalList.add(total);

            // 再向map集合中封装status参数,用于查询有效订单数
            map.put("status", Orders.COMPLETED);
            // 调用orderMapper层中的getOrdersStatisticsByMap方法查询单日有效订单数
            Integer valid = orderMapper.getOrdersStatisticsByMap(map);
            // 将查询的单日有效订单数累加到全时期有效订单数上
            validSum += valid;
            // 将查询到的单日有效订单数添加到单日有效订单集合中
            validList.add(valid);
        }

        // 3,使用上述的查询数据封装OrderReportVO对象并返回
        // 对全时期总订单数进行判断,若总订单数为0则订单完成率为0;若总订单数不为0,则使用有效订单数比上总订单数
        Double completionRate = 0.0;
        completionRate = totalSum == 0 ? completionRate : validSum.doubleValue() / totalSum;
        OrderReportVO orderReportVO = OrderReportVO.builder()
                // 使用StringUtils中的join方法将集合元素拼接为字符串,传入两个参数,第一个是集合,第二个是元素分割符
                .dateList(StringUtils.join(localDates, ","))
                .orderCountList(StringUtils.join(totalList, ","))
                .validOrderCountList(StringUtils.join(validList, ","))
                .totalOrderCount(totalSum)
                .validOrderCount(validSum)
                .orderCompletionRate(completionRate)
                .build();
        return orderReportVO;
    }

    /**
     * 统计前10的菜品或套餐
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getTop10(LocalDate begin, LocalDate end) {
        // 1,根据传入的开始/结束统计日期构造具体的统计时刻
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        // 2,根据构造的具体统计时间查询排名前10的菜品及对应的销量,并将其封装到GoodsSalesDTO中,
        // 10个GoodsSalesDTO封装给到list集合中返回
        List<GoodsSalesDTO> goodsList = orderMapper.getTop10(beginTime, endTime);

        // 3,将返回的list集合中的GoodsSalesDTO对象的商品名称和销量属性分别取出,并分别封装到两个list集合中
        List<String> names = new ArrayList<>();
        List<Integer> numbers = new ArrayList<>();

        for (GoodsSalesDTO goodsSalesDTO : goodsList) {
            names.add(goodsSalesDTO.getName());
            numbers.add(goodsSalesDTO.getNumber());
        }

        // 4,将上述查到的前10名菜品的名称和销量封装到SalesTop10ReportVO中并返回
        SalesTop10ReportVO vo = SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(names, ","))
                .numberList(StringUtils.join(numbers, ","))
                .build();
        return vo;
    }

    /**
     * 导出excel报表数据
     * @param response
     */
    @Override
    public void exportExcel(HttpServletResponse response) throws FileNotFoundException {
        // 1,将报表模板文件中需要的字段都查询出来
        // 1.1查询文件中的"概览数据"
        // 定义查询日期,查询近30天数据,需要从前30天到昨天(今天由于不确定是否过完,因此截至到昨天一共30天)
        LocalDate today = LocalDate.now();
        LocalDate endDay = today.minusDays(1);
        LocalDate beginDay = today.minusDays(30);

        // 查询的精确时刻
        LocalDateTime endTime = LocalDateTime.of(endDay, LocalTime.MAX);
        LocalDateTime beginTime = LocalDateTime.of(beginDay, LocalTime.MIN);
        // 使用workSpaceServiceImp中的getBusinessData方法来查询近30天的营业概览数据
        BusinessDataVO businessData = workspaceService.getBusinessData(beginTime, endTime);


        // 1.2 分别查询近30天中每一天的营业明细数据
        // 定义list集合用于存放每一天的查询结果
        List<BusinessDataVO> list = new ArrayList<>();
        for (int i = 1; i < 31; i++) {
            // 从后往前查询,从查询日的前一天开始查询,localDate代表本次循环要查询的日期
            LocalDate localDate = today.minusDays(i);
            // 拓展具体查询时刻,开始时刻是00:00:00,结束时刻为23:59:59
            LocalDateTime begin = LocalDateTime.of(localDate, LocalTime.MIN);
            LocalDateTime end = LocalDateTime.of(localDate, LocalTime.MAX);

            // 调用使用workSpaceServiceImp中的getBusinessData方法查询当日的明细,并将其放到list集合中
            BusinessDataVO vo = workspaceService.getBusinessData(begin, end);
            list.add(vo);
        }

        // 2,根据已经存在的报表模板在内存中新建一个报表excel对象(也即根据模板在内存在复制一份)
        // 新建输入流对象,将模板文件的文件流创建出来
        InputStream in = new FileInputStream(new File("sky-server/src/main/resources/template/运营数据报表模板.xlsx"));
        try {
            // 引入模板文件流并在内存中创建一个excel表(也即复制模板文件内容在内存中新生成一个excel表)
            XSSFWorkbook workbook = new XSSFWorkbook(in);
            // 取出该表的第一个sheet(也即报表所在的sheet)
            XSSFSheet info = workbook.getSheetAt(0);


            // 2.1 向表中添加概览数据
            // 向info中的第二行第二格(列)添加查询日期信息(查模板可得),索引从0开始,对应的序号需要减去1
            XSSFCell timeCell = info.getRow(1).getCell(1); // 得到第二行第二列的格子对象
            timeCell.setCellValue(today.minusDays(30)
                    +"至"+today.minusDays(1)); // 使用格子对象调用set方法设置格子对应的value

            // 分别向表中按位置添加概览数据businessData
            info.getRow(3).getCell(2).setCellValue(businessData.getTurnover());
            info.getRow(3).getCell(4).setCellValue(businessData.getOrderCompletionRate());
            info.getRow(3).getCell(6).setCellValue(businessData.getNewUsers());
            info.getRow(4).getCell(2).setCellValue(businessData.getValidOrderCount());
            info.getRow(4).getCell(4).setCellValue(businessData.getUnitPrice());


            // 2.2 向表中添加每日营业明细
            // 遍历30天的businessData对象,将对应的属性添加到表中对应的位置
            for (int i = 0; i < list.size(); i++) {
                BusinessDataVO vo = list.get(i);

                // 由于查询明细是是按照日期由近及远依次查询并放入集合中,所以这里写入行也是按照日期由近及远
                // 由于明细数据是从第8行开始的,因此行号索引要从7开始并且顺着i的增长递增;同样地,日期数据也是顺着i的增长递减
                info.getRow(i+7).getCell(1).setCellValue(String.valueOf(today.minusDays(i+1))); // 插入日期数据
                info.getRow(i+7).getCell(2).setCellValue(vo.getTurnover()); // 插入营业额
                info.getRow(i+7).getCell(3).setCellValue(vo.getValidOrderCount()); // 插入有效订单
                info.getRow(i+7).getCell(4).setCellValue(vo.getOrderCompletionRate()); // 订单完成率
                info.getRow(i+7).getCell(5).setCellValue(vo.getUnitPrice()); // 平均客单价
                info.getRow(i+7).getCell(6).setCellValue(vo.getNewUsers()); // 新增用户

            }

            // 3,将写入的excel传回给客户端服务器
            // 3.1 通过HttPServletResponse对象创建输出流
            ServletOutputStream outputStream = response.getOutputStream();
            // 将写入的excel通过输出流返回给客户端浏览器
            workbook.write(outputStream);

            // 3.2添加数据完成后,关闭输入流和poi资源(内存中的excel表)
            in.close();
            workbook.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
