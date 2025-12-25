package dev.kaiwen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import dev.kaiwen.dto.OrdersPaymentDTO;
import dev.kaiwen.dto.OrdersSubmitDTO;
import dev.kaiwen.entity.Orders;
import dev.kaiwen.vo.OrderSubmitVO;

public interface IOrderService extends IService<Orders> {
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);
    
    void payment(OrdersPaymentDTO ordersPaymentDTO);
    
    /**
     * 处理超时订单
     */
    void processTimeoutOrder();
    
    /**
     * 处理前一天未完成的订单：将前一天的所有未完成订单标记为已完成
     */
    void processDeliveryOrder();
}
