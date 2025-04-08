package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/setmeal")
@Slf4j
@Api(tags = "套餐管理接口")
public class SetmealController {
    @Autowired
    private SetmealService setmealService;


    /**
     * 套餐分页查询
     *
     * @param setmealPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("套餐分页查询")
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageResult pageResult = setmealService.page(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    @ApiOperation("根据套餐id查询套餐")
    public Result<SetmealVO> getById(@PathVariable Integer id){
        SetmealVO setmealVO = setmealService.getWithSetmealDishById(id);
        return Result.success(setmealVO);
    }


    /**
     * 根据套餐id修改套餐信息以及其包含的菜品数据
     * @param setmealDTO
     * @return
     */
    // 当管理端操作套餐数据(增加,删除,修改,停启用)后都需要清除redis中对应的缓存.
    // 使用@CacheEvict清除缓存,传入参数和@Cacheable一致,cacheName声明要删除缓存的一级目录,key声明要删除缓存的key,
    // 也可以使用allEntries=true把所有以"cacheNames"作为一级目录的缓存全部删除.
    @CacheEvict(cacheNames = "SetmealCache",allEntries = true)
    @PutMapping
    @ApiOperation("修改套餐")
    public Result updateWithSetmealDishes(@RequestBody SetmealDTO setmealDTO){
        setmealService.updateWithSetmealDishes(setmealDTO);
        return Result.success();
    }

    /**
     * 修改套餐的起售状态
     * @param status
     * @return
     */
    @CacheEvict(cacheNames = "SetmealCache",allEntries = true)
    @PostMapping("/status/{status}")
    @ApiOperation("起/停售套餐")
    public Result updateStatus(@PathVariable("status") Integer status, SetmealDTO setmealDTO){
        setmealDTO.setStatus(status);
        setmealService.updateStatus(setmealDTO);
        return Result.success();
    }

    /**
     * 按照套餐id批量删除套餐
     * @param ids
     * @return
     */
    @CacheEvict(cacheNames = "SetmealCache",allEntries = true)
    @DeleteMapping
    @ApiOperation("批量删除套餐")
    public Result deleteByIds(@RequestParam("ids")List<Long> ids){

        setmealService.deleteByIdsWithDishes(ids);
        return Result.success();
    }

    /**
     * 新增套餐以及套餐绑定的菜品(若有)
     * @param setmealDTO
     * @return
     */
    @CacheEvict(cacheNames = "SetmealCache",allEntries = true)
    @PostMapping
    @ApiOperation("新增套餐")
    public Result save(@RequestBody SetmealDTO setmealDTO){
        setmealService.saveWithDishes(setmealDTO);
        return Result.success();
    }
}
