package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishFlavorsMapper {

    /**
     * 批量插入口味数据
     * @param flavors
     */
    void insertBatct(List<DishFlavor> flavors);
}
