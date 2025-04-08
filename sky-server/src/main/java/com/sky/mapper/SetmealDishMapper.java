package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {
    List<Long> getSetmealIdByDishId(List<Long> ids);

    List<SetmealDish> getBySetmealId(Integer setmealId);


    /**
     * 根据套餐id集合在套餐菜品表中删除套餐产品关联信息
     * @param setmealIds
     */
    void deleteBySetmealId(List<Long> setmealIds);

    /**
     * 向套餐菜品表中添加套餐菜品关联数据
     * @param setmealDishes
     */
    void saveSetmealDishes(List<SetmealDish> setmealDishes);

}
