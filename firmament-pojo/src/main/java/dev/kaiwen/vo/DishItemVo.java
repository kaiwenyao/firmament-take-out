package dev.kaiwen.vo;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 菜品项视图对象.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DishItemVo implements Serializable {

  // 菜品名称
  private String name;

  // 份数
  private Integer copies;

  // 菜品图片
  private String image;

  // 菜品描述
  private String description;
}
