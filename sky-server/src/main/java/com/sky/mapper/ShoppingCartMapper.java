package com.sky.mapper;

import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {

    /**
     * 在购物车表中查询菜品/套餐数据
     *
     * @param shoppingCart
     * @return
     */
    List<ShoppingCart> list(ShoppingCart shoppingCart);

    /**
     * 修改购物车表中商品的数量
     *
     * @param existCart
     */
    @Update("update shopping_cart set number=#{number} where id=#{id}")
    void updateNumber(ShoppingCart existCart);

    /**
     * 添加购物车数据
     * @param shoppingCart
     */
    void save(ShoppingCart shoppingCart);

    /**
     * 依照用户id清除购物车信息
     *
     * @param userId
     */
    @Delete("delete from shopping_cart where user_id=#{id}")
    void deleteByUserId(Long userId);

    /**
     * 通过主键id进行删除
     */
    @Delete("delete from shopping_cart where id=#{id}")
    void deleteById(Long id);

    /**
     * 批量添加购物车数据
     * @param shoppingCartList
     */
    void saveBatch(List<ShoppingCart> shoppingCartList);
}
