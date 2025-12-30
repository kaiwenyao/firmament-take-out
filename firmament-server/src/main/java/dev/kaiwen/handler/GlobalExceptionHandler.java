package dev.kaiwen.handler;

import dev.kaiwen.exception.BaseException;
import dev.kaiwen.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
        log.error("业务异常：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 捕获MySQL数据库重复条目异常（如用户名重复、唯一约束冲突等）
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result<String> exceptionHandler(SQLIntegrityConstraintViolationException ex){
        log.error("数据库约束违反异常", ex);
        String message = ex.getMessage();
        if (message != null && message.contains("Duplicate entry")) {
            String[] split = message.split(" ");
            // 检查数组长度，防止越界
            if (split.length >= 3) {
                String username = split[2];
                // 移除可能的引号
                username = username.replace("'", "").replace("\"", "");
                String msg = username + ALREADY_EXIST;
                return Result.error(msg);
            } else {
                log.warn("无法解析重复条目异常信息: {}", message);
                return Result.error("数据已存在，请勿重复添加");
            }
        } else {
            return Result.error(UNKNOWN_ERROR);
        }
    }

    /**
     * 捕获空指针异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result<String> exceptionHandler(NullPointerException ex){
        log.error("空指针异常", ex);
        return Result.error("系统错误：数据不存在");
    }

    /**
     * 捕获参数非法异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result<String> exceptionHandler(IllegalArgumentException ex){
        log.error("参数非法异常：{}", ex.getMessage());
        // 不暴露详细异常信息，避免泄露内部实现细节
        return Result.error("参数错误，请检查输入");
    }

    /**
     * 捕获运行时异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result<String> exceptionHandler(RuntimeException ex){
        log.error("运行时异常", ex);
        // 不暴露详细异常信息，避免泄露内部实现细节（如数据库结构、文件路径等）
        return Result.error("系统错误，请联系管理员");
    }

    /**
     * 捕获所有其他异常（兜底处理）
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result<String> exceptionHandler(Exception ex){
        log.error("系统异常", ex);
        return Result.error(UNKNOWN_ERROR);
    }
}
