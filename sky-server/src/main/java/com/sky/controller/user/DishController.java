package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "C端-菜品浏览接口")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
//    @Cacheable(cacheNames = "DishCache", key="#categoryId")
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {

        // 在数据库查询之前先查询redis缓存,若缓存里存在则直接返回结果,若不存在再进行数据库查询,然后将查询结果插入到redis中以便下一次查询使用.
        ValueOperations valueOperations = redisTemplate.opsForValue();
        // 由于约定菜品信息是以字符串形式在redis中存储的且键值对的键是以"dish_{分类id}"的形式存在,因此需要使用分类id构造查询键
        String key = "dish_" + categoryId;
        // 由于约定菜品信息以字符串形式在redis中存储,在存储时使用序列化器将List<DishVO>转化为字符串形式存储,因此取出时也需要以List<DishVO>形式取出.
        List<DishVO> list = (List<DishVO>) valueOperations.get(key);

        // 判断查询返回的list是否为空(也即待查询菜品信息是否存在于redis中),若不为空说明redis中存在菜品信息,则直接响应返回
        if (list != null && list.size() > 0) {
            return Result.success(list);
        }
        // list为空则说明待查询菜品信息不在redis中,仍需执行数据库查询
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);//查询起售中的菜品

        list = dishService.listWithFlavor(dish);

        // 将新查询到的菜品信息缓存到redis中,同样使用约定的键key
        valueOperations.set(key, list);

        return Result.success(list);
    }

}
