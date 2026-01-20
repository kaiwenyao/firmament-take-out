package dev.kaiwen.dto;

import java.io.Serializable;
import lombok.Data;

/**
 * 订单拒绝数据传输对象.
 */
@Data
public class OrdersRejectionDto implements Serializable {

  private Long id;

  // 订单拒绝原因
  private String rejectionReason;

}
