package dev.kaiwen.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单提交视图对象.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSubmitVo implements Serializable {

  // 订单id
  private Long id;
  // 订单号
  private String orderNumber;
  // 订单金额
  private BigDecimal orderAmount;
  // 下单时间
  private LocalDateTime orderTime;
}
