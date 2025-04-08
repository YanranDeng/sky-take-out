package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 套餐业务实现
 */
@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;

    /**
     * 条件查询
     *
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     *
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult page(SetmealPageQueryDTO setmealPageQueryDTO) {
        // 设置分页参数
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        // 调用Napper层方法查询套餐
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);

        // 创建PageResult对象,并将page中的total和分页查询结果result写入其中,并返回PageResult对象.
        long total = page.getTotal();
        List<SetmealVO> result = page.getResult();
        PageResult pageResult = new PageResult(total, result);

        return pageResult;
    }

    @Override
    public SetmealVO getWithSetmealDishById(Integer id) {
        // 分两个查询sql进行查询,第一个是根据套餐id查询setmeal表,返回一个setmealVO对象,
        // 它封装除了List<SetmealDish>以外的所有setmealVO属性
        SetmealVO setmealVO = setmealMapper.getById(id);
        // 第二个是使用套餐id查询setmeal表左连接setmeal_dishes联表,查询返回的是List<SetmealDish>,该集合中封装了setmeal_dishes表中的字段.
        List<SetmealDish> list = setmealDishMapper.getBySetmealId(id);
        // 将List<SetmealDish>写入第一步查询的setmealVO对象中,然后返回
        setmealVO.setSetmealDishes(list);
        return setmealVO;
    }

    /**
     * 根据套餐id修改套餐信息以及其包含的菜品数据
     * @param setmealDTO
     * @return
     */
    @Override
    @Transactional
    public void updateWithSetmealDishes(SetmealDTO setmealDTO) {
        // 1 根据dto中的传入信息将setmeal表中的套餐信息更新
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);

        setmealMapper.updateById(setmeal);

        // 2 通过套餐id将setmeal_dishes表中的菜品数据删除,然后在将dto中的菜品信息重新添加
        Long id = setmealDTO.getId();
        List<Long> list = new ArrayList<>();
        list.add(id);
        setmealDishMapper.deleteBySetmealId(list);

        // 遍历新添加的菜品结合setmealDishes,给其中每一个菜品的setmealId都上套餐的id,以便于在setmeal_dishes表中将setmeal和对应的dishes对应
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();

        for (SetmealDish setmealDish : setmealDishes) {
            setmealDish.setSetmealId(id);
        }
        setmealDishMapper.saveSetmealDishes(setmealDishes);

    }

    /**
     * 修改套餐的起售状态
     * @param status
     * @return
     */
    @Override
    public void updateStatus(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        setmealMapper.updateById(setmeal);
    }

    /**
     * 按照套餐id批量删除套餐和套餐菜品表中的关联的菜品
     * @param ids
     * @return
     */
    @Override
    @Transactional
    public void deleteByIdsWithDishes(List<Long> ids) {
        // 先通过套餐id集合删除套餐表中的套餐数据
        setmealMapper.deleteByIds(ids);

        // 再通过套餐id集合删除套餐产品表中的数据(也即套餐关联的菜品数据)
        setmealDishMapper.deleteBySetmealId(ids);
    }

    /**
     * 新增套餐以及套餐绑定的菜品(若有)
     * @param dishDTO
     * @return
     */
    @Override
    @Transactional
    public void saveWithDishes(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);

        setmealMapper.save(setmeal);

        // 将套餐插入后,xml映射文件设置属性将插入套餐的主键id返回给setmeal对象,然后将setmeal对象的id属性取出,并将id赋值给套餐菜品对象setmealDish,
        // 这样就可以将套餐菜品表中的套餐id,菜品id等字段补齐,补齐后将套餐菜品对象插入到套餐菜品表中.
        Long id = setmeal.getId();
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        for (SetmealDish setmealDish : setmealDishes) {
            setmealDish.setSetmealId(id);
        }
        setmealDishMapper.saveSetmealDishes(setmealDishes);

    }
}
