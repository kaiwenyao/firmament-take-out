package dev.kaiwen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import dev.kaiwen.dto.ShoppingCartDto;
import dev.kaiwen.entity.ShoppingCart;

import java.util.List;


public interface ShoppingCartService extends IService<ShoppingCart> {


    void addShoppingCart(ShoppingCartDto shoppingCartDTO);

    /**
     * 减少购物车中商品数量
     * @param shoppingCartDTO 购物车DTO
     */
    void subShoppingCart(ShoppingCartDto shoppingCartDTO);

    List<ShoppingCart> showShoppingCart();

    /**
     * 清空购物车
     */
    void cleanShoppingCart();
}

