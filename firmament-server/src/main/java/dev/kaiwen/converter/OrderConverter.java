package dev.kaiwen.converter;

import dev.kaiwen.dto.OrdersSubmitDTO;
import dev.kaiwen.entity.Orders;
import org.mapstruct.Mapper;

/**
 * Order 转换器
 * 使用 MapStruct 自动生成实现类
 */
@Mapper(componentModel = "spring")
public interface OrderConverter {
    /**
     * DTO -> Entity (用于提交订单)
     * @param ordersSubmitDTO 订单提交DTO
     * @return 订单实体
     */
    Orders d2e(OrdersSubmitDTO ordersSubmitDTO);
}

