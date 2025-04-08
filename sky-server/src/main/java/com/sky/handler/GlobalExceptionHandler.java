package com.sky.handler;

import com.sky.constant.*;
import com.sky.exception.BaseException;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.LoginFailedException;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     *
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex) {
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 使用全局异常捕获SQLIntegrityConstraintViolationException异常,
     * 该异常是数据表被唯一约束修饰的字段信息重复后抛出
     *
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(SQLIntegrityConstraintViolationException ex) {
        // 获得异常的message字段
        String message = ex.getMessage().toString();
        log.info(message);
        // 判断异常信息是否含有Duplicate entry字段(也即提示重复字段信息的内容),
        // 若存在,则封装错误提示信息返回给前端
        if (message.contains("Duplicate entry")) {
            String username = message.split(" ")[2];
            String msg = username + MessageConstant.ALREADY_EXISTS;
            return Result.error(msg);
        }
        // 若不含有Duplicate entry信息,则异常为预料外异常,返回给前端
        else {
            return Result.error(MessageConstant.UNKNOWN_ERROR);
        }
    }

    /**
     * 套餐删除异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(DeletionNotAllowedException ex) {
        String message = ex.getMessage();
        log.info("错误信息为:{}", message);
        return Result.error(message);
    }

    /**
     * 微信登录异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(LoginFailedException ex) {
        String message = ex.getMessage();
        log.info("错误信息为:{}", message);
        return Result.error(message);
    }

}
