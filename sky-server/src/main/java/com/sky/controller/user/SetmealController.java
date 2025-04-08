package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.Setmeal;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController("userSetmealController")
@RequestMapping("/user/setmeal")
@Api(tags = "C端-套餐浏览接口")
public class SetmealController {
    @Autowired
    private SetmealService setmealService;

    /**
     * 条件查询
     *
     * @param categoryId
     * @return
     */
    // 使用springCache在redis缓存用户端的查询数据,
    // 在根据分类数据查询套餐的方法上使用@Cacheable注解,
    // 若redis中无缓存利用反射继续执行方法体查询数据库并返回数据,然后在将查询结果存入redis缓存中;
    // 若redis中有缓存,则直接根据@Cacheable中的参数在redis中查询缓存然后直接返回,不会执行查询方法.
    // 传入两个参数,cacheNames是缓存的一级名字,key是缓存的三级名字,利用这两个参数就可以构建一个"cacheNames::key"结构目录(在redis中":"表示层级)
    // 其中,key的写法有一定规则,如key必须是字符串形式且字符串中需为spring-explain-language,也即以"#"开头且后面跟随具体参数;
    // spring-explain-language可以根据传入变量动态计算目录,类似与格式化字符串.
    // springCache是以注解参数key为key,以方法返回值为value,然后构建一个string类型数据存入redis中,
    // 例如,下面方法的redis缓存就是以"#{categoryId}"为key,以"Result.success(list)"为value的字符串存放在SetmealCache::categoryId路径下
    @Cacheable(cacheNames = "SetmealCache",key = "#categoryId")
    @GetMapping("/list")
    @ApiOperation("根据分类id查询套餐")
    public Result<List<Setmeal>> list(Long categoryId) {
        Setmeal setmeal = new Setmeal();
        setmeal.setCategoryId(categoryId);
        setmeal.setStatus(StatusConstant.ENABLE);

        List<Setmeal> list = setmealService.list(setmeal);
        return Result.success(list);
    }

    /**
     * 根据套餐id查询包含的菜品列表
     *
     * @param id
     * @return
     */
    @GetMapping("/dish/{id}")
    @ApiOperation("根据套餐id查询包含的菜品列表")
    public Result<List<DishItemVO>> dishList(@PathVariable("id") Long id) {
        List<DishItemVO> list = setmealService.getDishItemById(id);
        return Result.success(list);
    }
}
