package dev.kaiwen.converter;

import dev.kaiwen.entity.OrderDetail;
import dev.kaiwen.entity.ShoppingCart;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * OrderDetail 转换器. 使用 MapStruct 自动生成实现类.
 */
@Mapper
public interface OrderDetailConverter {

  OrderDetailConverter INSTANCE = Mappers.getMapper(OrderDetailConverter.class);

  /**
   * ShoppingCart -> OrderDetail (用于将购物车条目转换为订单明细).
   *
   * @param shoppingCart 购物车条目
   * @return 订单明细
   */
  @Mapping(target = "id", ignore = true)  // id 由数据库自动生成
  @Mapping(target = "orderId", ignore = true)
  // orderId 需要手动设置
  OrderDetail cart2Detail(ShoppingCart shoppingCart);

  /**
   * 批量转换：ShoppingCart 列表 -> OrderDetail 列表.
   *
   * @param shoppingCartList 购物车列表
   * @return 订单明细列表
   */
  List<OrderDetail> cartList2DetailList(List<ShoppingCart> shoppingCartList);
}

