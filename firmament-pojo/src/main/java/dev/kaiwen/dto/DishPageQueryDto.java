package dev.kaiwen.dto;

import java.io.Serializable;
import lombok.Data;

/**
 * 菜品分页查询数据传输对象.
 */
@Data
public class DishPageQueryDto implements Serializable {

  private int page;

  private int pageSize;

  private String name;

  // 分类id
  private Integer categoryId;

  // 状态 0表示禁用 1表示启用
  private Integer status;

}
