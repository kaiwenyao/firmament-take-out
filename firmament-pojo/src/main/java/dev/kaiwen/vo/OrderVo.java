package dev.kaiwen.vo;

import dev.kaiwen.entity.OrderDetail;
import dev.kaiwen.entity.Orders;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 订单视图对象.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderVo extends Orders {

  // 订单菜品信息
  private String orderDishes;

  // 订单详情
  private List<OrderDetail> orderDetailList;

}
