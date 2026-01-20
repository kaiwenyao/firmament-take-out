package dev.kaiwen.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 订单分页查询数据传输对象.
 */
@Data
public class OrdersPageQueryDto implements Serializable {

  private int page;

  private int pageSize;

  private String number;

  private String phone;

  private Integer status;

  @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime beginTime;

  @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime endTime;

  private Long userId;

}
