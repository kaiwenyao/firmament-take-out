package dev.kaiwen.dto;

import java.io.Serializable;
import lombok.Data;

/**
 * 订单取消数据传输对象.
 */
@Data
public class OrdersCancelDto implements Serializable {

  private Long id;
  // 订单取消原因
  private String cancelReason;

}
