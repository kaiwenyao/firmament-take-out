package dev.kaiwen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import dev.kaiwen.dto.ShoppingCartDTO;
import dev.kaiwen.entity.ShoppingCart;

import java.util.List;


public interface IShoppingCartService extends IService<ShoppingCart> {


    void addShoppingCart(ShoppingCartDTO shoppingCartDTO);

    List<ShoppingCart> showShoppingCart();

    /**
     * 清空购物车
     */
    void cleanShoppingCart();
}
