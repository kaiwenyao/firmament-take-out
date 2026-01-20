package dev.kaiwen.task;

import dev.kaiwen.service.OrderService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务类.
 * 用于处理超时订单和派送订单的定时任务.
 */

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderTask {

  private final OrderService orderService;

  /**
   * 定时处理超时订单.
   * 每分钟执行一次，处理超时未支付的订单.
   */
  @Scheduled(cron = "0 * * * * ?") // 每分钟一次
  public void processTimeoutOrder() {
    log.info("定时处理超时订单：{}", LocalDateTime.now());
    orderService.processTimeoutOrder();
  }

  /**
   * 定时处理前一天未完成的订单.
   * 每天凌晨1点执行一次，处理前一天未完成的派送订单.
   */
  @Scheduled(cron = "0 0 1 * * *")
  // @Scheduled(cron = "3 * * * * ?") // 每分钟一次（测试用）
  public void processDeliveryOrder() {
    log.info("定时处理前一天未完成的订单:{}", LocalDateTime.now());
    orderService.processDeliveryOrder();
  }
}
