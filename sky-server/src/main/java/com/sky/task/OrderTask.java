package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 定时处理超时未支付的订单
     * 每隔1分钟执行一次该方法检查用户15分钟内未支付的订单,
     * 然后将这些订单的状态改为已取消.
     */
    @Scheduled(cron = "0 * * * * ? ") // 使用cron表达式定义每分钟执行一次该方法
//    @Scheduled(cron = "0/5 * * * * ?  ") // 使用cron表达式定义每分钟执行一次该方法
    public void ckeckOutOfTime(){
        log.info("检查超时订单:{}",LocalDateTime.now());
        // 定义截至下单时间,将该时间与表中订单下单时间对比,若订单下单时间早于(小于)则说明该订单已超时
        LocalDateTime flagTime = LocalDateTime.now().minusMinutes(15);
        List<Orders> list = orderMapper.getByStatusAndOrderTime(Orders.PENDING_PAYMENT,flagTime);

        // 判断是否存在超时订单
        if (list!=null && !list.isEmpty()){
            // 遍历查询列表,并更新orders对象属性值,并更新订单表
            list.forEach(new Consumer<Orders>() {
                @Override
                public void accept(Orders orders) {
                    orders.setStatus(Orders.CANCELLED);
                    orders.setCancelTime(LocalDateTime.now());
                    orders.setCancelReason("订单超时取消");
                    orderMapper.update(orders);
                }
            });
        }
    }


    /**
     * 定时检查长时间未确认的订单
     * 每天早上5点5秒执行该方法检查并处理未确定订单,
     * 频率每天一次
     */
    @Scheduled(cron = "0 0 2 * * ?  ") // 定义每天早上2点执行该方法,频率为一天
//    @Scheduled(cron = "3/5 * * * * ?  ")
    public void checkUncertainOrders(){
        // 定义节点时间,因为该方法是每天上午2点被执行,而凌晨两点店铺大都打烊,因此可以检查并处理昨天的未确定订单,
        // 因此节点时间选为0点,故节点时间就是拿方法执行时当前时间减去两个小时,即为0点.
        LocalDateTime flagTime = LocalDateTime.now().minusHours(2);

        // 查询下单时间在昨天23:59:59之前且订单状态为"派送中"的订单
        List<Orders> list = orderMapper.getByStatusAndOrderTime(Orders.DELIVERY_IN_PROGRESS, flagTime);

        // 判断查询列表是否为空
        if (list!=null && !list.isEmpty()){
            // 遍历查询列表,并更新orders对象属性值,并更新订单表
            list.forEach(new Consumer<Orders>() {
                @Override
                public void accept(Orders orders) {
                    orders.setStatus(Orders.COMPLETED);
                    orders.setDeliveryTime(LocalDateTime.now());
                    orderMapper.update(orders);
                }
            });
        }
    }
}
