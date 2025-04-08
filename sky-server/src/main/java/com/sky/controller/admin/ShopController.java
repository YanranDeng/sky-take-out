package com.sky.controller.admin;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;

// 给admin的ShopController起一个别名,用于区分user中的ShopController,以免两者在IoC容器中重名报错
@RestController("adminShopController")
@Slf4j
@Api(tags = "营业状态接口")
@RequestMapping("/admin/shop")
public class ShopController {
    // 注入redis模板对象
    @Autowired
    private RedisTemplate redisTemplate;

    // 将重复使用的字符串定义成常量
    private static final String key = "Shop_Status";

    /**
     * 修改营业状态
     * 传递的路径参数是将要修改为的营业状态,约定1为营业,0为打烊
     *
     * @param status
     * @return
     */
    @ApiOperation("修改营业状态")
    @PutMapping("/{status}")
    public Result updateStatus(@PathVariable Integer status) {
        log.info("营业状态修改为:{}", status == 1 ? "营业中" : "打烊中");
        // 使用redis模板创建redis字符串格式的接口对象
        ValueOperations valueOperations = redisTemplate.opsForValue();

        // 使用接口对象向redis中添加一条名为Shop_Status的字符串,声明为营业状态
        valueOperations.set(key, status);
        return Result.success();

    }


    /**
     * 查询营业状态
     * @return
     */
    @GetMapping("/status")
    @ApiOperation("查询营业状态")
    public Result<Integer> findStatus() {

        // 使用redis模板创建redis字符串格式的接口对象
        ValueOperations valueOperations = redisTemplate.opsForValue();

        // 使用接口对象获取redis存储的营业状态,并返回一个Integer值返回
        Integer status = (Integer) valueOperations.get(key);

        log.info("当前的营业状态为:{}", status == 1 ? "营业中" : "打烊中");

        return Result.success(status);
    }
}
