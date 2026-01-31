package dev.kaiwen.handler;

import static dev.kaiwen.constant.AutoFillConstant.CREATE_TIME;
import static dev.kaiwen.constant.AutoFillConstant.CREATE_USER;
import static dev.kaiwen.constant.AutoFillConstant.UPDATE_TIME;
import static dev.kaiwen.constant.AutoFillConstant.UPDATE_USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import dev.kaiwen.context.BaseContext;
import dev.kaiwen.entity.Category;
import java.time.LocalDateTime;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * {@link AutoFillMetaObjectHandler} 单元测试.
 */
class AutoFillMetaObjectHandlerTest {

  private AutoFillMetaObjectHandler handler;

  private Level savedLogLevel;

  @BeforeEach
  void setUp() {
    handler = new AutoFillMetaObjectHandler();
    // 初始化 Category 的 TableInfo，使 strictInsertFill/strictUpdateFill 能通过 findTableInfo 解析
    MapperBuilderAssistant assistant =
        new MapperBuilderAssistant(new MybatisConfiguration(), "");
    TableInfoHelper.initTableInfo(assistant, Category.class);
    // 单元测试中会触发自动填充日志，临时关闭避免大量输出
    Logger logger = (Logger) LoggerFactory.getLogger(AutoFillMetaObjectHandler.class);
    savedLogLevel = logger.getLevel();
    logger.setLevel(Level.OFF);
  }

  @AfterEach
  void tearDown() {
    BaseContext.removeCurrentId();
    Logger logger = (Logger) LoggerFactory.getLogger(AutoFillMetaObjectHandler.class);
    logger.setLevel(savedLogLevel);
  }

  /** 创建包装 Category 的 MetaObject，用于 insert/update 填充测试. */
  private static MetaObject createMetaObject(Category target) {
    return SystemMetaObject.forObject(target);
  }

  @Nested
  @DisplayName("插入操作自动填充 insertFill")
  class InsertFillTest {

    @Test
    void insertFillSetsCreateTimeUpdateTimeCreateUserUpdateUser() {
      // 1. 准备测试数据 - 空实体，四个字段均为 null
      Category category = new Category();
      MetaObject metaObject = createMetaObject(category);
      Long currentId = 100L;
      BaseContext.setCurrentId(currentId);

      // 2. 执行测试
      handler.insertFill(metaObject);

      // 3. 验证结果 - 四个公共字段均应被填充
      Object createTime = metaObject.getValue(CREATE_TIME);
      Object createUser = metaObject.getValue(CREATE_USER);
      Object updateTime = metaObject.getValue(UPDATE_TIME);
      Object updateUser = metaObject.getValue(UPDATE_USER);

      assertNotNull(createTime);
      assertNotNull(updateTime);
      assertEquals(LocalDateTime.class, createTime.getClass());
      assertEquals(LocalDateTime.class, updateTime.getClass());
      assertEquals(currentId, createUser);
      assertEquals(currentId, updateUser);
    }

    @Test
    void insertFillWhenCurrentIdIsNullFillsWithNullForUserFields() {
      Category category = new Category();
      MetaObject metaObject = createMetaObject(category);
      // 不设置 BaseContext，getCurrentId() 为 null

      handler.insertFill(metaObject);

      // 时间字段仍会被填充（LocalDateTime.now()），用户字段为 null
      assertNotNull(metaObject.getValue(CREATE_TIME));
      assertNotNull(metaObject.getValue(UPDATE_TIME));
      assertEquals(null, metaObject.getValue(CREATE_USER));
      assertEquals(null, metaObject.getValue(UPDATE_USER));
    }
  }

  @Nested
  @DisplayName("更新操作自动填充 updateFill")
  class UpdateFillTest {

    @Test
    void updateFillSetsUpdateTimeAndUpdateUser() {
      // 1. 准备测试数据
      Category category = new Category();
      MetaObject metaObject = createMetaObject(category);
      Long currentId = 200L;
      BaseContext.setCurrentId(currentId);

      // 2. 执行测试
      handler.updateFill(metaObject);

      // 3. 验证结果 - 仅更新 updateTime、updateUser
      Object updateTime = metaObject.getValue(UPDATE_TIME);
      Object updateUser = metaObject.getValue(UPDATE_USER);

      assertNotNull(updateTime);
      assertEquals(LocalDateTime.class, updateTime.getClass());
      assertEquals(currentId, updateUser);
    }

    @Test
    void updateFillDoesNotSetCreateTimeOrCreateUser() {
      Category category = new Category();
      MetaObject metaObject = createMetaObject(category);
      BaseContext.setCurrentId(300L);

      handler.updateFill(metaObject);

      // 更新填充不涉及 createTime、createUser，应仍为 null
      assertEquals(null, metaObject.getValue(CREATE_TIME));
      assertEquals(null, metaObject.getValue(CREATE_USER));
    }
  }
}
