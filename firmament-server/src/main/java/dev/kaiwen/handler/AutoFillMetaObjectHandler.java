package dev.kaiwen.handler;

import static dev.kaiwen.constant.AutoFillConstant.CREATE_TIME;
import static dev.kaiwen.constant.AutoFillConstant.CREATE_USER;
import static dev.kaiwen.constant.AutoFillConstant.UPDATE_TIME;
import static dev.kaiwen.constant.AutoFillConstant.UPDATE_USER;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import dev.kaiwen.context.BaseContext;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

/**
 * 自定义元数据对象处理器. 逻辑：MP 在执行 insert/update 时会自动调用这里的方法.
 */
@Component
@Slf4j
public class AutoFillMetaObjectHandler implements MetaObjectHandler {

  /**
   * 插入操作自动填充.
   */
  @Override
  public void insertFill(MetaObject metaObject) {
    log.info("开始进行公共字段自动填充(insert)...");
    LocalDateTime now = LocalDateTime.now();
    Long currentId = BaseContext.getCurrentId();

    // 为 4 个字段赋值 (注意：这里是属性名，不是数据库字段名)
    this.strictInsertFill(metaObject, CREATE_TIME, LocalDateTime.class, now);
    this.strictInsertFill(metaObject, CREATE_USER, Long.class, currentId);
    this.strictInsertFill(metaObject, UPDATE_TIME, LocalDateTime.class, now);
    this.strictInsertFill(metaObject, UPDATE_USER, Long.class, currentId);
  }

  /**
   * 更新操作自动填充.
   */
  @Override
  public void updateFill(MetaObject metaObject) {
    log.info("开始进行公共字段自动填充(update)...");

    // 更新时只需要填充这两个
    this.strictUpdateFill(metaObject, UPDATE_TIME, LocalDateTime.class, LocalDateTime.now());
    this.strictUpdateFill(metaObject, UPDATE_USER, Long.class, BaseContext.getCurrentId());
  }
}