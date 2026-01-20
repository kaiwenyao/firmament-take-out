package dev.kaiwen.dto;

import dev.kaiwen.entity.SetmealDish;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 套餐数据传输对象.
 */
@Data
public class SetmealDto implements Serializable {

  private Long id;

  // 分类id
  private Long categoryId;

  // 套餐名称
  private String name;

  // 套餐价格
  private BigDecimal price;

  // 状态 0:停用 1:启用
  private Integer status;

  // 描述信息
  private String description;

  // 图片
  private String image;

  // 套餐菜品关系
  private List<SetmealDish> setmealDishes = new ArrayList<>();

}
