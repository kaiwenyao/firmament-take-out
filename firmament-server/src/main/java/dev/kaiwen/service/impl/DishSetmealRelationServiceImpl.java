package dev.kaiwen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import dev.kaiwen.constant.StatusConstant;
import dev.kaiwen.entity.Dish;
import dev.kaiwen.entity.Setmeal;
import dev.kaiwen.entity.SetmealDish;
import dev.kaiwen.mapper.DishMapper;
import dev.kaiwen.mapper.SetmealDishMapper;
import dev.kaiwen.mapper.SetmealMapper;
import dev.kaiwen.service.DishSetmealRelationService;
import dev.kaiwen.service.SetmealDishService;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 菜品和套餐关联关系检查服务实现. 通过直接使用 Mapper 层查询，避免 Service 层之间的循环依赖.
 */
@Service
@RequiredArgsConstructor
public class DishSetmealRelationServiceImpl implements DishSetmealRelationService {

  private final SetmealDishService setmealDishService;
  private final SetmealDishMapper setmealDishMapper;
  private final DishMapper dishMapper;
  private final SetmealMapper setmealMapper;

  /**
   * 检查菜品是否被起售的套餐使用.
   *
   * @param dishId 菜品ID
   * @return true 如果有起售的套餐在使用该菜品，false 否则
   */
  @Override
  public boolean hasEnabledSetmealUsingDish(Long dishId) {
    // 1. 查询使用该菜品的套餐ID列表
    // 使用 Wrappers + mapper 方式查询
    LambdaQueryWrapper<SetmealDish> setmealDishWrapper = Wrappers.lambdaQuery(SetmealDish.class)
        .eq(SetmealDish::getDishId, dishId);
    List<SetmealDish> setmealDishes = setmealDishMapper.selectList(setmealDishWrapper);

    // 2. 提取套餐ID列表（去重）
    List<Long> setmealIds = extractSetmealIds(setmealDishes);
    if (setmealIds.isEmpty()) {
      return false;
    }

    // 3. 批量查询套餐信息（直接使用 Mapper，避免 Service 层循环依赖）
    // 使用 Wrappers + mapper 方式查询
    LambdaQueryWrapper<Setmeal> wrapper = Wrappers.lambdaQuery(Setmeal.class)
        .in(Setmeal::getId, setmealIds);
    List<Setmeal> setmealList = setmealMapper.selectList(wrapper);

    // 4. 检查是否有起售的套餐
    return setmealList.stream()
        .anyMatch(setmeal -> StatusConstant.ENABLE.equals(setmeal.getStatus()));
  }

  /**
   * 检查套餐内是否有停售的菜品.
   *
   * @param setmealId 套餐ID
   * @return true 如果套餐内有停售的菜品，false 否则
   */
  @Override
  public boolean hasDisabledDishInSetmeal(Long setmealId) {
    // 1. 查询套餐关联的菜品ID列表
    // 使用 Wrappers + mapper 方式查询
    LambdaQueryWrapper<SetmealDish> setmealDishWrapper = Wrappers.lambdaQuery(SetmealDish.class)
        .eq(SetmealDish::getSetmealId, setmealId);
    List<SetmealDish> setmealDishes = setmealDishMapper.selectList(setmealDishWrapper);

    // 2. 提取菜品ID列表
    List<Long> dishIds = extractDishIds(setmealDishes);
    if (dishIds.isEmpty()) {
      return false;
    }

    // 3. 批量查询菜品信息（直接使用 Mapper，避免 Service 层循环依赖）
    // 使用 Wrappers + mapper 方式查询
    LambdaQueryWrapper<Dish> wrapper = Wrappers.lambdaQuery(Dish.class)
        .in(Dish::getId, dishIds);
    List<Dish> dishList = dishMapper.selectList(wrapper);

    // 4. 检查是否有停售的菜品
    return dishList.stream()
        .anyMatch(dish -> StatusConstant.DISABLE.equals(dish.getStatus()));
  }

  /**
   * 从套餐菜品关联关系中提取套餐ID列表.
   *
   * @param setmealDishes 套餐菜品关联关系列表
   * @return 套餐ID列表（去重）
   */
  private List<Long> extractSetmealIds(List<SetmealDish> setmealDishes) {
    if (setmealDishes == null || setmealDishes.isEmpty()) {
      return List.of();
    }
    return setmealDishes.stream()
        .map(SetmealDish::getSetmealId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
  }

  /**
   * 从套餐菜品关联关系中提取菜品ID列表.
   *
   * @param setmealDishes 套餐菜品关联关系列表
   * @return 菜品ID列表（去重）
   */
  private List<Long> extractDishIds(List<SetmealDish> setmealDishes) {
    if (setmealDishes == null || setmealDishes.isEmpty()) {
      return List.of();
    }
    return setmealDishes.stream()
        .map(SetmealDish::getDishId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
  }
}

