package dev.kaiwen.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import dev.kaiwen.context.BaseContext;
import dev.kaiwen.converter.ShoppingCartConverter;
import dev.kaiwen.dto.ShoppingCartDto;
import dev.kaiwen.entity.Dish;
import dev.kaiwen.entity.Setmeal;
import dev.kaiwen.entity.ShoppingCart;
import dev.kaiwen.mapper.ShoppingCartMapper;
import dev.kaiwen.service.DishService;
import dev.kaiwen.service.SetmealService;
import dev.kaiwen.service.ShoppingCartService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 购物车服务实现类.
 * 提供购物车的添加、减少、查看、清空等功能.
 */
@Service
@RequiredArgsConstructor
public class ShoppingCartServiceImpl extends
    ServiceImpl<ShoppingCartMapper, ShoppingCart> implements ShoppingCartService {

  private final SetmealService setmealService;
  private final DishService dishService;

  /**
   * 添加商品到购物车.
   *
   * @param shoppingCartDto 购物车DTO
   */
  @Override
  public void addShoppingCart(ShoppingCartDto shoppingCartDto) {
    // 1. 查询购物车中是否已经存在这个条目
    List<ShoppingCart> list = findShoppingCartByDto(shoppingCartDto);

    // 3. 如果已存在，更新数量
    if (list != null && !list.isEmpty()) {
      ShoppingCart cart = list.get(0);
      cart.setNumber(cart.getNumber() + 1);
      // 更新购物车
      this.updateById(cart);
    } else {
      // 4. 如果不存在，创建新的购物车条目
      // 4.1 使用 Converter 将 DTO 转换为 Entity
      ShoppingCart shoppingCart = ShoppingCartConverter.INSTANCE.d2e(shoppingCartDto);
      shoppingCart.setUserId(BaseContext.getCurrentId());
      shoppingCart.setNumber(1);

      // 4.2 判断是菜品还是套餐，查询对应信息并设置属性
      Long dishId = shoppingCartDto.getDishId();
      Long setmealId = shoppingCartDto.getSetmealId();

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

  /**
   * 减少购物车中商品数量.
   *
   * @param shoppingCartDto 购物车DTO
   */
  @Override
  public void subShoppingCart(ShoppingCartDto shoppingCartDto) {
    // 1. 查找购物车中对应的条目
    List<ShoppingCart> list = findShoppingCartByDto(shoppingCartDto);

    // 3. 如果找到对应的购物车条目
    if (list != null && !list.isEmpty()) {
      ShoppingCart cart = list.get(0);
      // 如果数量大于1，则减1
      if (cart.getNumber() > 1) {
        cart.setNumber(cart.getNumber() - 1);
        this.updateById(cart);
      } else {
        // 如果数量等于1，则删除该条目
        this.removeById(cart.getId());
      }
    }
  }

  /**
   * 查看购物车.
   *
   * @return 当前用户的购物车列表
   */
  @Override
  public List<ShoppingCart> showShoppingCart() {
    Long userId = BaseContext.getCurrentId();

    // 使用 MyBatis Plus 的 lambdaQuery 查询当前用户的购物车列表
    return lambdaQuery()
        .eq(ShoppingCart::getUserId, userId)
        .orderByAsc(ShoppingCart::getCreateTime)
        .list();
  }

  /**
   * 清空购物车. 删除当前用户的所有购物车记录.
   */
  @Override
  public void cleanShoppingCart() {
    // 1. 获取当前用户ID
    Long userId = BaseContext.getCurrentId();

    // 2. 使用 MyBatis Plus 的 lambdaUpdate 删除当前用户的所有购物车记录
    // 语义：DELETE FROM shopping_cart WHERE user_id = ?
    lambdaUpdate()
        .eq(ShoppingCart::getUserId, userId)
        .remove();
  }

  /**
   * 根据购物车DTO查询购物车条目.
   *
   * @param shoppingCartDto 购物车DTO
   * @return 购物车条目列表
   */
  private List<ShoppingCart> findShoppingCartByDto(ShoppingCartDto shoppingCartDto) {
    Long userId = BaseContext.getCurrentId();
    return lambdaQuery()
        .eq(ShoppingCart::getUserId, userId)
        .eq(shoppingCartDto.getDishId() != null, ShoppingCart::getDishId,
            shoppingCartDto.getDishId())
        .eq(shoppingCartDto.getSetmealId() != null, ShoppingCart::getSetmealId,
            shoppingCartDto.getSetmealId())
        .eq(shoppingCartDto.getDishFlavor() != null, ShoppingCart::getDishFlavor,
            shoppingCartDto.getDishFlavor())
        .list();
  }
}
