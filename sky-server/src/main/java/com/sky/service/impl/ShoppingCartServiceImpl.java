package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;


    /**
     * 添加购物车
     *
     * @param shoppingCartDTO
     */
    @Override
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);

        // 在对象中补充当前用户id,这是由于不同用户的不同购物车数据是互相独立的,后续查询购物车时需要使用用户id来查询该用户的购物车数据.
        shoppingCart.setUserId(BaseContext.getCurrentId());

/*         1,使用封装好的shoppingcart对象在shopping_cart表中查询,若已经存在该菜品/套餐(也即该用户已经将该菜品/套餐添加进购物车),
         这时若要添加相同的菜品/套餐,只需要在原有的数据上更新数量字段即可*/
        // 传入shoppingcart对象,查询所有满足字段信息的购物车数据,并将其封装到List集合中.
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);

        // 判断待添加商品在购物车表中是否存在,若存在则只需在原有的基础上对其数据进行跟进加上1即可
        if (list != null && !list.isEmpty()) {
            // 取出查询返回集合的第一个元素,根据当前业务规范,一个用户对于同一个商品在购物车表中只会产生一条数据且每次前端请求添加商品也只会添加菜品/套餐中的一种,
            // 因此上述查询业务得到的list集合有且只有一个元素
            ShoppingCart existCart = list.get(0);
            existCart.setNumber(existCart.getNumber() + 1);
            // 将修改后的shoppingcart对象传入updateNumber方法中修改数量
            shoppingCartMapper.updateNumber(existCart);
        }


        // 2,若该套餐/菜品在数据表中不存在(也即该用户第一次添加该菜品),则需要判断前端传入的菜品还是套餐,
        // 根据对应的对象在数据表中查询菜品/套餐信息(价格,图片等),然后查询信息也封装到shoppingcart对象中,继而添加到shopping_cart表中.
        else {
            // shoppingCart对象中取出dishId和setmealId
            Long dishId = shoppingCart.getDishId();
            Long setmealId = shoppingCart.getSetmealId();
            // 判断dishId是否为空,由于前端每次只能向购物车添加一个商品,因此dishId和setmealId必须有一个有值且另一个为空.
            // 若dishId不为空,则表示本次添加的商品为菜品,则需要根据dishId在菜品表查询菜品信息,并将该信息添加到购物车表中.
            if (dishId != null) {
                // 在菜品表中查询菜品
                Dish dish = dishMapper.getDishById(dishId);

                // 将菜品信息补充到shoppingcart对象中
                shoppingCart.setName(dish.getName());
                shoppingCart.setAmount(dish.getPrice());
                shoppingCart.setImage(dish.getImage());
            }
            // 若传入商品不为dish,则为setmeal,与上述一致,补充setmeal信息至shoppingcart对象中
            else {
                SetmealVO setmealVO = setmealMapper.getById(Math.toIntExact(setmealId));
                // 将套餐信息补充到shoppingcart对象中
                shoppingCart.setName(setmealVO.getName());
                shoppingCart.setAmount(setmealVO.getPrice());
                shoppingCart.setImage(setmealVO.getImage());
            }
            // 填充上述两个分支语句中共有的逻辑
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCart.setNumber(1);

            // 将菜品/套餐信息插入到购物车表中
            shoppingCartMapper.save(shoppingCart);

        }
    }

    /**
     * 查询当前用户的购物车
     *
     * @return
     */
    @Override
    public List<ShoppingCart> list() {
        // 创建ShoppingCart对象,并将当前用户的用户id赋值.
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(BaseContext.getCurrentId());
        // 查询当前用户的购物车信息,并返回
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        return list;
    }

    /**
     * 清除当前用户的购物车
     */
    @Override
    public void delete() {
        // 获取当前微信用户的id
        Long id = BaseContext.getCurrentId();
        // 根据id清除当前用户的购物车
        shoppingCartMapper.deleteByUserId(id);
    }

    /**
     * 减少购物车中的一个商品数量
     *
     * @param shoppingCartDTO
     */
    @Override
    public void decreaseNumber(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        shoppingCart.setUserId(BaseContext.getCurrentId());

        // 1,首先向查询待减少的商品在购物车表中的信息
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        ShoppingCart existCart = list.get(0);


        // 2,其次根据上述查询的商品信息对其数量进行更新
        // 将商品的数量减少1
        existCart.setNumber(existCart.getNumber() - 1);

        if (existCart.getNumber() > 0) {
            shoppingCartMapper.updateNumber(existCart);
        } else if (existCart.getNumber() == 0) {
            shoppingCartMapper.deleteById(existCart.getId());
        }
    }
}
