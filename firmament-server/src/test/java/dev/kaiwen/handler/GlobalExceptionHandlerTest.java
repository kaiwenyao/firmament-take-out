package dev.kaiwen.handler;

import static dev.kaiwen.constant.MessageConstant.ALREADY_EXIST;
import static dev.kaiwen.constant.MessageConstant.UNKNOWN_ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import dev.kaiwen.exception.BaseException;
import dev.kaiwen.result.Result;
import java.sql.SQLIntegrityConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * {@link GlobalExceptionHandler} 单元测试.
 */
class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler handler;

  private Level savedLogLevel;

  @BeforeEach
  void setUp() {
    handler = new GlobalExceptionHandler();
    // 单元测试中故意触发异常，临时关闭 GlobalExceptionHandler 的日志，避免堆栈等大量输出
    Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
    savedLogLevel = logger.getLevel();
    logger.setLevel(Level.OFF);
  }

  @AfterEach
  void tearDown() {
    Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
    logger.setLevel(savedLogLevel);
  }

  @Nested
  @DisplayName("业务异常 BaseException")
  class BaseExceptionHandlerTest {

    @Test
    void exceptionHandlerReturnsErrorWithMessage() {
      // 1. 准备测试数据
      String message = "账号不存在";
      BaseException ex = new BaseException(message);

      // 2. 执行测试
      Result<String> result = handler.exceptionHandler(ex);

      // 3. 验证结果
      assertNotNull(result);
      assertEquals(0, result.getCode());
      assertEquals(message, result.getMsg());
    }

    @Test
    void exceptionHandlerWithEmptyMessage() {
      BaseException ex = new BaseException("");

      Result<String> result = handler.exceptionHandler(ex);

      assertNotNull(result);
      assertEquals(0, result.getCode());
      assertEquals("", result.getMsg());
    }
  }

  @Nested
  @DisplayName("SQL 约束违反异常 SQLIntegrityConstraintViolationException")
  class SqlIntegrityConstraintViolationExceptionHandlerTest {

    @Test
    void duplicateEntryWithMatchedValueReturnsValuePlusAlreadyExist() {
      // 1. 准备测试数据 - 标准 MySQL 重复条目消息格式
      String message = "Duplicate entry 'admin' for key 'idx_username'";
      SQLIntegrityConstraintViolationException ex =
          new SQLIntegrityConstraintViolationException(message);

      // 2. 执行测试
      Result<String> result = handler.exceptionHandler(ex);

      // 3. 验证结果 - 应提取 'admin' 并拼接 "已存在"
      assertNotNull(result);
      assertEquals(0, result.getCode());
      assertEquals("admin" + ALREADY_EXIST, result.getMsg());
    }

    @Test
    void duplicateEntryWithChineseValue() {
      String message = "Duplicate entry '测试用户' for key 'idx_name'";
      SQLIntegrityConstraintViolationException ex =
          new SQLIntegrityConstraintViolationException(message);

      Result<String> result = handler.exceptionHandler(ex);

      assertNotNull(result);
      assertEquals(0, result.getCode());
      assertEquals("测试用户" + ALREADY_EXIST, result.getMsg());
    }

    @Test
    void duplicateEntryWhenRegexDoesNotMatchReturnsGenericMessage() {
      // 消息包含 "Duplicate entry" 但格式异常，正则无法提取值
      String message = "Duplicate entry invalid format no quotes";
      SQLIntegrityConstraintViolationException ex =
          new SQLIntegrityConstraintViolationException(message);

      Result<String> result = handler.exceptionHandler(ex);

      assertNotNull(result);
      assertEquals(0, result.getCode());
      assertEquals("数据已存在，请勿重复添加", result.getMsg());
    }

    @Test
    void messageWithoutDuplicateEntryReturnsUnknownError() {
      String message = "Some other constraint violation";
      SQLIntegrityConstraintViolationException ex =
          new SQLIntegrityConstraintViolationException(message);

      Result<String> result = handler.exceptionHandler(ex);

      assertNotNull(result);
      assertEquals(0, result.getCode());
      assertEquals(UNKNOWN_ERROR, result.getMsg());
    }

    @Test
    void messageNullReturnsUnknownError() {
      SQLIntegrityConstraintViolationException ex =
          new SQLIntegrityConstraintViolationException();

      Result<String> result = handler.exceptionHandler(ex);

      assertNotNull(result);
      assertEquals(0, result.getCode());
      assertEquals(UNKNOWN_ERROR, result.getMsg());
    }
  }

  @Nested
  @DisplayName("空指针异常 NullPointerException")
  class NullPointerExceptionHandlerTest {

    @Test
    void exceptionHandlerReturnsDataNotExistMessage() {
      NullPointerException ex = new NullPointerException("some detail");

      Result<String> result = handler.exceptionHandler(ex);

      assertNotNull(result);
      assertEquals(0, result.getCode());
      assertEquals("系统错误：数据不存在", result.getMsg());
    }
  }

  @Nested
  @DisplayName("参数非法异常 IllegalArgumentException")
  class IllegalArgumentExceptionHandlerTest {

    @Test
    void exceptionHandlerReturnsGenericMessageNotExposingDetail() {
      String detail = "internal implementation detail";
      IllegalArgumentException ex = new IllegalArgumentException(detail);

      Result<String> result = handler.exceptionHandler(ex);

      assertNotNull(result);
      assertEquals(0, result.getCode());
      assertEquals("参数错误，请检查输入", result.getMsg());
    }
  }

  @Nested
  @DisplayName("运行时异常 RuntimeException")
  class RuntimeExceptionHandlerTest {

    @Test
    void exceptionHandlerReturnsContactAdminMessage() {
      RuntimeException ex = new RuntimeException("database structure leaked");

      Result<String> result = handler.exceptionHandler(ex);

      assertNotNull(result);
      assertEquals(0, result.getCode());
      assertEquals("系统错误，请联系管理员", result.getMsg());
    }
  }

  @Nested
  @DisplayName("通用异常 Exception")
  class ExceptionHandlerTest {

    @Test
    void exceptionHandlerReturnsUnknownError() {
      Exception ex = new Exception("any checked exception");

      Result<String> result = handler.exceptionHandler(ex);

      assertNotNull(result);
      assertEquals(0, result.getCode());
      assertEquals(UNKNOWN_ERROR, result.getMsg());
    }
  }
}
