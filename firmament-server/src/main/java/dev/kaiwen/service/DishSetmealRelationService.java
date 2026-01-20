package dev.kaiwen.service;

/**
 * 菜品和套餐关联关系检查服务.
 * 用于处理菜品和套餐之间的业务关联检查，避免循环依赖.
 */
public interface DishSetmealRelationService {

  /**
   * 检查菜品是否被起售的套餐使用.
   *
   * @param dishId 菜品ID
   * @return true 如果有起售的套餐在使用该菜品，false 否则
   */
  boolean hasEnabledSetmealUsingDish(Long dishId);

  /**
   * 检查套餐内是否有停售的菜品.
   *
   * @param setmealId 套餐ID
   * @return true 如果套餐内有停售的菜品，false 否则
   */
  boolean hasDisabledDishInSetmeal(Long setmealId);
}

