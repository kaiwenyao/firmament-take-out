package dev.kaiwen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import dev.kaiwen.dto.ShoppingCartDTO;
import dev.kaiwen.entity.ShoppingCart;


public interface IShoppingCartService extends IService<ShoppingCart> {


    void addShoppingCart(ShoppingCartDTO shoppingCartDTO);
}
