package dev.kaiwen.handler;

import dev.kaiwen.exception.BaseException;
import dev.kaiwen.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import dev.kaiwen.constant.MessageConstant;

import java.sql.SQLIntegrityConstraintViolationException;

import static dev.kaiwen.constant.MessageConstant.ALREADY_EXIST;
import static dev.kaiwen.constant.MessageConstant.UNKNOWN_ERROR;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result<String> exceptionHandler(BaseException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 捕获MySQL数据库重复条目异常（如用户名重复、唯一约束冲突等）
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result<String> exceptionHandler(SQLIntegrityConstraintViolationException ex){
        log.error("保存员工失败：存在重复条目");
        String message = ex.getMessage();
        if (message.contains("Duplicate entry")) {
            String[] split = message.split(" ");
            String username = split[2];
            String msg = username + ALREADY_EXIST;
            return Result.error(msg);
        } else {
            return Result.error(UNKNOWN_ERROR);
        }
    }
}
