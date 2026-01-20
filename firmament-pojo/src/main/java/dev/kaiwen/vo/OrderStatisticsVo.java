package dev.kaiwen.vo;

import java.io.Serializable;
import lombok.Data;

/**
 * 订单统计视图对象.
 */
@Data
public class OrderStatisticsVo implements Serializable {

  // 待接单数量
  private Integer toBeConfirmed;

  // 待派送数量
  private Integer confirmed;

  // 派送中数量
  private Integer deliveryInProgress;
}
