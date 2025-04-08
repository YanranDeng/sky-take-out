package com.sky.annotation;

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定义一个注解,设置一个属性用来标记被该注解标记的Mapper中的方法执行的数据库操作是什么,
 * 该属性的取值为一个枚举,枚举有两个属性,一个是Update,另一个是Insert
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoFill {
    OperationType value();
}
