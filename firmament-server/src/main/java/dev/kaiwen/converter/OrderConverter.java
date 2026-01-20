package dev.kaiwen.converter;

import dev.kaiwen.dto.OrdersSubmitDto;
import dev.kaiwen.entity.Orders;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Order 转换器
 * 使用 MapStruct 自动生成实现类
 */
@Mapper
public interface OrderConverter {
    
    OrderConverter INSTANCE = Mappers.getMapper(OrderConverter.class);
    
    /**
     * DTO -> Entity (用于提交订单)
     * @param ordersSubmitDTO 订单提交DTO
     * @return 订单实体
     */
    Orders d2e(OrdersSubmitDto ordersSubmitDTO);
}

