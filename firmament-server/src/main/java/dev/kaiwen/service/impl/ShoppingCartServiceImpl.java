package dev.kaiwen.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import dev.kaiwen.context.BaseContext;
import dev.kaiwen.converter.ShoppingCartConverter;
import dev.kaiwen.dto.ShoppingCartDTO;
import dev.kaiwen.entity.Dish;
import dev.kaiwen.entity.Setmeal;
import dev.kaiwen.entity.ShoppingCart;
import dev.kaiwen.mapper.ShoppingCartMapper;
import dev.kaiwen.service.IDishService;
import dev.kaiwen.service.ISetmealService;
import dev.kaiwen.service.IShoppingCartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShoppingCartServiceImpl extends ServiceImpl<ShoppingCartMapper, ShoppingCart> implements IShoppingCartService {
    private final ISetmealService setmealService;
    private final IDishService dishService;
    private final ShoppingCartConverter shoppingCartConverter;

    @Override
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        // 1. 获取当前用户ID
        Long userId = BaseContext.getCurrentId();
        
        // 2. 构建查询条件，查看购物车中是否已经存在这个条目
        // 根据 userId, dishId/setmealId, dishFlavor 来查询
        List<ShoppingCart> list = lambdaQuery()
                .eq(ShoppingCart::getUserId, userId)
                .eq(shoppingCartDTO.getDishId() != null, ShoppingCart::getDishId, shoppingCartDTO.getDishId())
                .eq(shoppingCartDTO.getSetmealId() != null, ShoppingCart::getSetmealId, shoppingCartDTO.getSetmealId())
                .eq(shoppingCartDTO.getDishFlavor() != null, ShoppingCart::getDishFlavor, shoppingCartDTO.getDishFlavor())
                .list();

        // 3. 如果已存在，更新数量
        if (list != null && !list.isEmpty()) {
            ShoppingCart cart = list.get(0);
            cart.setNumber(cart.getNumber() + 1);
            // 更新购物车
            this.updateById(cart);
        } else {
            // 4. 如果不存在，创建新的购物车条目
            // 4.1 使用 Converter 将 DTO 转换为 Entity
            ShoppingCart shoppingCart = shoppingCartConverter.d2e(shoppingCartDTO);
            shoppingCart.setUserId(userId);
            shoppingCart.setNumber(1);
            
            // 4.2 判断是菜品还是套餐，查询对应信息并设置属性
            Long dishId = shoppingCartDTO.getDishId();
            Long setmealId = shoppingCartDTO.getSetmealId();
            
            if (dishId != null) {
                // 是菜品，查询菜品信息
                Dish dish = dishService.getById(dishId);
                if (dish != null) {
                    shoppingCart.setName(dish.getName());
                    shoppingCart.setImage(dish.getImage());
                    shoppingCart.setAmount(dish.getPrice());
                }
            } else if (setmealId != null) {
                // 是套餐，查询套餐信息
                Setmeal setmeal = setmealService.getById(setmealId);
                if (setmeal != null) {
                    shoppingCart.setName(setmeal.getName());
                    shoppingCart.setImage(setmeal.getImage());
                    shoppingCart.setAmount(setmeal.getPrice());
                }
            }
            
            // 4.3 插入新的购物车条目
            this.save(shoppingCart);
        }
    }
}
