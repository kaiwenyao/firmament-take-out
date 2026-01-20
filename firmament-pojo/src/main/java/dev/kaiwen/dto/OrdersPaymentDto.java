package dev.kaiwen.dto;

import java.io.Serializable;
import lombok.Data;

/**
 * 订单支付数据传输对象.
 */
@Data
public class OrdersPaymentDto implements Serializable {

  // 订单号
  private String orderNumber;

  // 付款方式
  private Integer payMethod;

}
