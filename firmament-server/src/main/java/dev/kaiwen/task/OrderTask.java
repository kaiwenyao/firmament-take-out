package dev.kaiwen.task;


import dev.kaiwen.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 定时任务类
 *
 */

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderTask {

    private final OrderService orderService;

    @Scheduled(cron = "0 * * * * ?") // 每分钟一次
    public void processTimeoutOrder() {
        log.info("定时处理超时订单：{}", LocalDateTime.now());
        orderService.processTimeoutOrder();
    }



    @Scheduled(cron = "0 0 1 * * *")
//    @Scheduled(cron = "3 * * * * ?") // 每分钟一次
    public void processDeliveryOrder() {
        log.info("定时处理前一天未完成的订单:{}",  LocalDateTime.now());
        orderService.processDeliveryOrder();
    }
}
