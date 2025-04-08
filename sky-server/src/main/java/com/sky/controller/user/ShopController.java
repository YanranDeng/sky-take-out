package com.sky.controller.user;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 给user的ShopController起一个别名,用于区分admin中的ShopController,以免两者在IoC容器中重名报错
@RestController("userShopController")
@Slf4j
@Api(tags = "营业状态接口")
@RequestMapping("/user/shop")
public class ShopController {

    private static final String key = "Shop_Status";

    @Autowired
    private RedisTemplate redisTemplate;


    /**
     * 查询营业状态
     *
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
