package dev.kaiwen.dto;

import java.io.Serializable;
import lombok.Data;

/**
 * 分类数据传输对象.
 */
@Data
public class CategoryDto implements Serializable {

  // 主键
  private Long id;

  // 类型 1 菜品分类 2 套餐分类
  private Integer type;

  // 分类名称
  private String name;

  // 排序
  private Integer sort;

}
