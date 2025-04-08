package com.sky.context;


/**
 * 定义一个工具类BaseContext,目的是创建在一个线程的内存区中实现定义,获取,删除一个局部变量
 */
public class BaseContext {

    public static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    public static Long getCurrentId() {
        return threadLocal.get();
    }

    public static void removeCurrentId() {
        threadLocal.remove();
    }

}
