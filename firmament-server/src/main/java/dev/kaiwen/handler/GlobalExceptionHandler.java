package dev.kaiwen.handler;

import dev.kaiwen.exception.BaseException;
import dev.kaiwen.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     * 使用正则表达式提取重复值，更稳健地处理不同MySQL版本和语言配置
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result<String> exceptionHandler(SQLIntegrityConstraintViolationException ex){
        log.error("数据库约束违反异常", ex);
        String message = ex.getMessage();

        if (message != null && message.contains("Duplicate entry")) {
            // 使用正则表达式提取单引号中的重复值
            // 匹配模式：Duplicate entry 'value' for key 'key_name'
            Pattern pattern = Pattern.compile("Duplicate entry '([^']+)'");
            Matcher matcher = pattern.matcher(message);

            if (matcher.find()) {
                // 成功提取到重复的值
                String duplicateValue = matcher.group(1);
                log.warn("检测到重复数据: {}", duplicateValue);
                return Result.error(duplicateValue + ALREADY_EXIST);
            } else {
                // 正则表达式未匹配，返回通用消息
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
