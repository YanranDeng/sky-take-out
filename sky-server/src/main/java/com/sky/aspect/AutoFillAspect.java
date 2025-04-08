package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.awt.font.OpenType;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 定义切面类,用以自动填充公共字段(创建时间,修改时间,创建人,修改人)
 * 该切面定义在update和insert操作上,定义在mapper层操作之前,也即在service层调用mapper层之前,将传入实体类中缺失的公共字段填充
 */
@Component
@Aspect
@Slf4j
public class AutoFillAspect {

    // 定义切入点
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut() {
    }

    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint) {
        // 1,获取切点方法的注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        AutoFill annotation = method.getAnnotation(AutoFill.class);
        OperationType value = annotation.value();

        // 2,获取切点方法的参数(也即传入的employee对象)
        // 默认是将切点方法所有参数放入一个数组中,由于一般默认是将传入employee对象放到传入参数第一个,
        // 因此通过args的索引可以取出employee对象
        Object[] args = joinPoint.getArgs();

        // 判断arg参数数组是否为空,若为空直接返回并结束程序
        if (args == null || args.length == 0) {
            return;
        }

        Object entity = args[0];

        // 3,设置公共字段值
        LocalDateTime now = LocalDateTime.now();
        Long id = BaseContext.getCurrentId();

        // 4,通过反射来给传入对象的公共字段赋值
        // 判断切点方法的注解并通过反射给对象的公共字段赋值
        if (value == OperationType.INSERT) {
            try {
                // 4.2 取出传入对象类(Employee类)的公共字段对应的set方法
                Method setCreateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                // 4.2 通过上述取出的set方法给传入对象的公共字段赋值
                setCreateTime.invoke(entity, now);
                setCreateUser.invoke(entity, id);
                setUpdateTime.invoke(entity, now);
                setUpdateUser.invoke(entity, id);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else if (value == OperationType.UPDATE) {
            try {
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                setUpdateTime.invoke(entity, now);
                setUpdateUser.invoke(entity, id);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


}
