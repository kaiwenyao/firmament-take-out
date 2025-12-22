package dev.kaiwen.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import dev.kaiwen.context.BaseContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 自定义元数据对象处理器
 * 逻辑：MP 在执行 insert/update 时会自动调用这里的方法
 */
@Component
@Slf4j
public class AutoFillMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入操作自动填充
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        log.info("开始进行公共字段自动填充(insert)...");
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();

        // 为 4 个字段赋值 (注意：这里是属性名，不是数据库字段名)
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "createUser", Long.class, currentId);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateUser", Long.class, currentId);
    }

    /**
     * 更新操作自动填充
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        log.info("开始进行公共字段自动填充(update)...");

        // 更新时只需要填充这两个
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updateUser", Long.class, BaseContext.getCurrentId());
    }
}