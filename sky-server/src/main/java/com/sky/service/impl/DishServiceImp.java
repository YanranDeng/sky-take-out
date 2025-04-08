package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.FlavorsMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@Service
@Slf4j
public class DishServiceImp implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private FlavorsMapper flavorsMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveWithFlavors(DishDTO dishDTO) {
        log.info("添加菜品为:{}", dishDTO);
        // 创建一个新的dish对象,用于将菜品数据传递到mapper层
        Dish dish = new Dish();
        // 将dishdto中共有数据拷贝到dish对象中
        BeanUtils.copyProperties(dishDTO, dish);
        // 将dish传递到mapper层的saveWithFlavors方法中,用以在数据表中添加该菜品数据
        dishMapper.saveWithFlavors(dish);


        // 取出dishdto对象中的Flavors属性(也即一个装着DishFlavor对象的集合),
        // 其中DishFlavor对象是口味类别对象(例如甜味,辣味等),而它封装了4个属性,分别是主键id,外键菜品id,口味类别名称,每个类别的细分(如甜味有微糖,少糖等)
        List<DishFlavor> flavors = dishDTO.getFlavors();

        // 判断菜品数据中是否包含口味数据,若前端未传入口味数据则跳过.
        if (flavors != null && flavors.size() > 0) {
            // 将dish表插入数据返回的主键id取出
            Long dishId = dish.getId();

            // 遍历flavors列表取出每一个DishFlavor对象并将dishId赋值给DishFlavor中.
            // 这样做的目的是:由于DishFlavor对象中封装着4个属性,而主键id和dish外键id在前端传递的数据中是没有的,
            // 而主键id在数据表中是自动递增的无需关心,而dish外键id需要手动填入(也即声明这一个口味是哪一个菜品关联的),
            // 一个菜品可能对于多个口味,因此需要在口味表中声明外键id来关联菜品表.
            flavors.forEach(new Consumer<DishFlavor>() {
                @Override
                public void accept(DishFlavor dishFlavor) {
                    dishFlavor.setDishId(dishId);
                }
            });

            // 调用flavors的mapper层方法,将flavors集合传入
            flavorsMapper.saveWithFlavors(flavors);
        }

    }

    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        log.info("分页参数为:{}", dishPageQueryDTO);
        // 定义分页参数,传入两个参数,分别是要分页页码(也即要查询第几页),以及每页查询条数
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());

        // 在分页插件后调用分页查询方法,传入dto对象,返回page对象,该对象是一个"加强版"List集合;
        // 它其中存放两个属性,一个是查询条数,另一个是存放查询对象的List集合.
        Page<DishVO> page = dishMapper.page(dishPageQueryDTO);
        // 新建PageResult对象作为分页查询统一返回类,封装两个属性,分别是查询条数以及查询数据集合.
        PageResult pageResult = new PageResult(page.getTotal(), page.getResult());
        return pageResult;
    }

    @Transactional
    @Override
    public void deleteBatch(List<Long> ids) {
        // 1 判断待删除菜品是否在售,若在售则不可删除,只需抛出异常,全局异常处理器处理返回
        List<Integer> status = dishMapper.getStatusById(ids);
        log.info(status.toString());
        if (status.contains(StatusConstant.ENABLE)) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
        }
        // 2 判断待删除菜品是否和套餐关联,若与套餐关联则不可删除,抛出异常
        List<Long> setMealId = setmealDishMapper.getSetmealIdByDishId(ids);
        if (setMealId.size() > 0 || setMealId != null) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        // 3 若1和2都满足,也即该菜品不与任何套餐关联且不在售,则可以删除
        dishMapper.deleteByIds(ids);
        // 4 删除产品表中的菜品时要将其对应的口味也从口味表中删除(若有),需要使用事务控制
        flavorsMapper.deleteByIds(ids);
    }

    @Override
    public DishVO getDishWithFlavor(Long id) {
        Dish dish = dishMapper.getDishById(id);
        List<DishFlavor> dishFlavor = flavorsMapper.getFlavorByDishId(id);

        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavor);

        return dishVO;
    }

    /**
     * 修改菜品信息需要两步操作:首先是根据传入的菜品信息修改菜品表;其次是将该菜品对应的口味表同步修改.
     *
     * @param dishDTO
     */
    @Override
    @Transactional
    public void updateDishWithFlavor(DishDTO dishDTO) {
        // 1 修改菜品表
        // 将dto的属性复制到dish对象中
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        // 调用update方法按id修改数据
        dishMapper.updateDishById(dish);

        // 2 将该菜品的对应口味也一并更新修改,修改分为两部分,首先将原有的口味数据全部删除,其次将新的口味数据添加到口味表中.

        // 将待修改的菜品id集合传递给deleteByIds方法,该方法将按照菜品id删除对应口味
        List<Long> list = new ArrayList<>();
        Long id = dishDTO.getId();
        Collections.addAll(list, id);
        flavorsMapper.deleteByIds(list);

        // 待原有的口味删除后将新添加的口味插入到口味表
        List<DishFlavor> flavors = dishDTO.getFlavors();
        // 判断新传的口味是否为空,若不为空则执行插入操作
        if (flavors != null && flavors.size() > 0) {
            // 由于前端在传递口味对象时主键id和dishId是没有传递的,
            // 因此需要遍历口味集合,将dishId添加到口味对象中,而主键id是在数据表中自增的无需操作
            flavors.forEach(flavor -> flavor.setDishId(id));
            flavorsMapper.saveWithFlavors(flavors);
        }
    }

    /**
     * 条件查询菜品和口味
     *
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.list(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d, dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = flavorsMapper.getFlavorByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }

    /**
     * 按照分类id查询菜品
     *
     * @param dishDTO
     * @return
     */
    @Override
    public List<Dish> getByCategoryId(DishDTO dishDTO) {

        List<Dish> list = dishMapper.getDishByCategoryId(dishDTO);
        return list;
    }


    /**
     * 启用/停用菜品
     * 若停用菜品则需将包含此菜品的套餐一并停用
     *
     * @param dish
     */
    @Override
    @Transactional
    public void updateDishStatus(Dish dish) {
        // 根据封装折菜品id和菜品状态的dish对象来修改菜品状态
        dishMapper.updateDishById(dish);

        // 若传入的菜品状态为"0"停售,则需要连同它关联的套餐一起停售
        if (dish.getStatus().equals(StatusConstant.DISABLE)) {
            // 新建list集合用于存放菜品id
            List<Long> list = new ArrayList<>();
            list.add(dish.getId());
            // 将菜品id集合传入getSetmealIdByDishId方法,根据菜品id查询与其关联的套餐id,并将套餐id置入集合中
            List<Long> setmealId = setmealDishMapper.getSetmealIdByDishId(list);


            // 新建setmeal对象
            Setmeal setmeal = new Setmeal();
            // 遍历上述步骤得到的套餐id集合,将套餐id和套餐状态(禁用)赋值给setmeal对象,并将赋值后的setmeal对象传给updateById方法,
            // 通过套餐id更新套餐状态
            for (Long l : setmealId) {
                setmeal.setStatus(StatusConstant.DISABLE);
                setmeal.setId(l);
                setmealMapper.updateById(setmeal);
            }
/*            // 将上述得到的套餐id集合传入updateStatus中,根据套餐id修改套餐状态
            setmealMapper.updateStatus(setmealId);*/
        }
    }
}
