package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@Slf4j
@Api(tags = "菜品管理接口")
@RequestMapping("/admin/dish")
public class DishController {

    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 添加菜品
     *
     * @param dishDTO
     * @return
     */
    @ApiOperation("添加菜品接口")
    @PostMapping
    public Result saveWithFlavors(@RequestBody DishDTO dishDTO) {
        dishService.saveWithFlavors(dishDTO);

        // 在添加菜品后数据库发生改变,为了保证数据一致性,原本在redis中的缓存数据因为和数据库数据出现不一致的情况,所以就不能使用了,需要删除
        // 构造待删除缓存的key,使用约定的"dish_"加上该菜品所属的分类id,这样就可以把该菜品所在分类的redis缓存删除,
        // 小程序再次查询时就直接查询数据库的更新数据,并将新数据再次写入redis缓存中.
        String key = "dish_" + dishDTO.getCategoryId();
        cleanCache(key);

        return Result.success();
    }


    /**
     * 分页查询菜品
     */
    @GetMapping("/page")
    @ApiOperation("分页查询")
    public Result<PageResult> pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 批量删除菜品
     *
     * @param ids
     * @return
     */

    @DeleteMapping
    @ApiOperation("批量删除菜品")
    public Result deleteBatch(@RequestParam List<Long> ids) {
        dishService.deleteBatch(ids);

        // 批量删除涉及数据库数据改变且涉及多个分类的数据,因此为了方便,在执行该操作时将redis中的所有分类菜品数据全部删除.
        cleanCache("dish_*");   // "dish_*"是使用通配符来表示所有以"dish_"开头的key
        return Result.success();
    }

    // TODO 未完待续

    /**
     * 修改菜品
     * 在该请求业务(即更新菜品信息)被发出之前,前端需要向后端发出三个相关请求,
     * 分别是菜品回显,口味回显(getDishWithFlavor)和套餐/菜品分类查询(根据前端传入的待修改菜品的category_id属性查询),用于菜品类型回显
     *
     * @param dishDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改菜品")
    public Result updateById(@RequestBody DishDTO dishDTO) {
        dishService.updateDishWithFlavor(dishDTO);

        // 修改菜品由于涉及修改菜品分类,也可能涉及多个分类,因此也将redis中所有菜品分类缓存清除
        cleanCache("dish_*");   // "dish_*"是使用通配符来表示所有以"dish_"开头的key
        return Result.success();
    }

    /**
     * 按菜品id查询菜品以及对应的口味信息
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("按菜品id查询菜品以及对应的口味信息")
    public Result<DishVO> getDishWithFlavor(@PathVariable Long id) {
        DishVO dishVO = dishService.getDishWithFlavor(id);
        return Result.success(dishVO);
    }

    /**
     * 按照分类id查询菜品
     * @param dishDTO
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("按照分类id查询菜品")
    public Result<List<Dish>> getByCategoryId(DishDTO dishDTO) {
        List<Dish> list = dishService.getByCategoryId(dishDTO);
        return Result.success(list);
    }


    /**
     * 启用/停用菜品
     * 若停用某菜品,则连同包含该菜品的套餐一并停用
     * @param status
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("启用/停用菜品")
    public Result updateStatus(@PathVariable("status") Integer status,DishDTO dishDTO ){
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        dish.setStatus(status);


        dishService.updateDishStatus(dish);
        return Result.success();
    }



    /**
     * 清除redis中的缓存
     * 在管理端执行菜品信息的增,删,改,启/停用等操作时数据库发生改变,为了保证数据一致性,
     * 原本在redis中的缓存数据因为和数据库数据出现不一致的情况,所以就不能使用了,需要删除
     * 传入参数是需要清除的缓存键值对的键或键的通配符
     */
    private void cleanCache(String pattern) {
        // 通过keys方法将需要删除的缓存的key查询出来
        Set keys = redisTemplate.keys(pattern);
        // 根据传入的key集合参数缓存
        redisTemplate.delete(keys);

    }
}
