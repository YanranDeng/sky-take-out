package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.entity.SetmealDish;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {
    /**
     * 根据菜品id得到对应的套餐id
     * @param dishIds
     * @return
     */
    List<Long> getSetmealIdsByDishIds(List<Long> dishIds);

    void insertBatch(List<SetmealDish> setmealDishes);

    @Select("select dish_id from setmeal_dish where setmeal_id = #{setmealId} ")
    List<Long> getDishIdsBySetmealId(Long setmealId);

    void deleteBySetmealIds(List<Long> setmealIds);

    @Select("select * from setmeal_dish where setmeal_id = #{setmealId} ")
    List<SetmealDish> getBySetmealId(Long setmealId);
}
