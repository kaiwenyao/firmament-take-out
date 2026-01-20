package dev.kaiwen.dto;

import java.io.Serializable;
import lombok.Data;

/**
 * 购物车数据传输对象.
 */
@Data
public class ShoppingCartDto implements Serializable {

  private Long dishId;
  private Long setmealId;
  private String dishFlavor;

}
