package dev.kaiwen.converter;

import dev.kaiwen.dto.ShoppingCartDTO;
import dev.kaiwen.entity.ShoppingCart;
import org.mapstruct.Mapper;

/**
 * ShoppingCart 转换器
 * 使用 MapStruct 自动生成实现类
 */
@Mapper(componentModel = "spring")
public interface ShoppingCartConverter {
    /**
     * DTO -> Entity (用于新增购物车条目)
     * @param shoppingCartDTO 购物车DTO
     * @return 购物车实体
     */
    ShoppingCart d2e(ShoppingCartDTO shoppingCartDTO);
}

