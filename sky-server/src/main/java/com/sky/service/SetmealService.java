package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.result.PageResult;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface SetmealService {

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    List<Setmeal> list(Setmeal setmeal);

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    List<DishItemVO> getDishItemById(Long id);

    PageResult page(SetmealPageQueryDTO setmealPageQueryDTO);

    SetmealVO getWithSetmealDishById(Integer id);

    void updateWithSetmealDishes(SetmealDTO setmealDTO);

    void updateStatus(SetmealDTO setmealDTO);

    void deleteByIdsWithDishes(List<Long> ids);

    void saveWithDishes(SetmealDTO setmealDTO);
}
