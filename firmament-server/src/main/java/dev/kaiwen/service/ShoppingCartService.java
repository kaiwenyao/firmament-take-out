package dev.kaiwen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import dev.kaiwen.dto.ShoppingCartDto;
import dev.kaiwen.entity.ShoppingCart;
import java.util.List;

/**
 * 购物车服务接口.
 */
public interface ShoppingCartService extends IService<ShoppingCart> {

  /**
   * 添加商品到购物车.
   *
   * @param shoppingCartDto 购物车DTO
   */
  void addShoppingCart(ShoppingCartDto shoppingCartDto);

  /**
   * 减少购物车中商品数量.
   *
   * @param shoppingCartDto 购物车DTO
   */
  void subShoppingCart(ShoppingCartDto shoppingCartDto);

  /**
   * 查看购物车.
   *
   * @return 购物车列表
   */
  List<ShoppingCart> showShoppingCart();

  /**
   * 清空购物车.
   */
  void cleanShoppingCart();
}

