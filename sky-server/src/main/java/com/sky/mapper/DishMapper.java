package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DishMapper {

    /**
     * 根据分类id查询菜品数量
     *
     * @param categoryId
     * @return
     */
    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    @AutoFill(OperationType.INSERT)
    void saveWithFlavors(Dish dish);

    Page<DishVO> page(DishPageQueryDTO dishPageQueryDTO);


    List<Integer> getStatusById(List<Long> ids);

    void deleteByIds(List<Long> ids);

    @Select("select id, name, category_id, price, image, description, status, create_time, update_time, create_user, update_user from dish where id = #{id}")
    Dish getDishById(Long id);

    // 给updateDishById方法进行AutoFill注解,该方法在执行时会被aop通知方法接管以便于给dish对象填充更新时间和更新人等公共字段
    @AutoFill(OperationType.UPDATE)
    void updateDishById(Dish dish);

    /**
     * 通过dish对象的属性查找dish表中的dish对象,并将它们封装到List集合中.
     * 其中传入dish对象中包含两个初始属性,分别是categoryId和Status.
     * 但是为了后续便于扩展该方法还是通过xml文件的形式写入动态sql
     *
     * @param dish
     * @return
     */
    List<Dish> list(Dish dish);

    /**
     * 根据分类id查询菜品
     *
     * @param dishDTO
     * @return
     */
    @Select("select * from dish where category_id=#{categoryId}")
    List<Dish> getDishByCategoryId(DishDTO dishDTO);

    /**
     * 根据条件统计菜品数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);
}
