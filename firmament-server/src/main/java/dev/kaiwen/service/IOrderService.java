package dev.kaiwen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import dev.kaiwen.dto.OrdersSubmitDTO;
import dev.kaiwen.entity.Orders;
import dev.kaiwen.vo.OrderSubmitVO;

public interface IOrderService extends IService<Orders> {
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);
}
