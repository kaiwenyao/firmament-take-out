package dev.kaiwen.dto;

import java.io.Serializable;
import lombok.Data;

/**
 * 分类分页查询数据传输对象.
 */
@Data
public class CategoryPageQueryDto implements Serializable {

  // 页码
  private int page;

  // 每页记录数
  private int pageSize;

  // 分类名称
  private String name;

  // 分类类型 1菜品分类  2套餐分类
  private Integer type;

}
