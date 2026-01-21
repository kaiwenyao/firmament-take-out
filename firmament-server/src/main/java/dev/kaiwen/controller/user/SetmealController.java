package dev.kaiwen.controller.user;

import dev.kaiwen.constant.StatusConstant;
import dev.kaiwen.entity.Setmeal;
import dev.kaiwen.result.Result;
import dev.kaiwen.service.SetmealService;
import dev.kaiwen.vo.DishItemVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Setmeal controller for client side.
 */
@RestController("userSetmealController")
@RequestMapping("/user/setmeal")
@Slf4j
@Tag(name = "C端-套餐浏览接口")
@RequiredArgsConstructor
public class SetmealController {

  private final SetmealService setmealService;

  /**
   * Get setmeals by category ID.
   *
   * @param categoryId The category ID.
   * @return The list of setmeals in the category.
   */
  @GetMapping("/list")
  @Operation(summary = "根据分类id查询套餐")
  @Cacheable(cacheNames = "setmealCache", key = "#categoryId") // setmealCache::100
  public Result<List<Setmeal>> list(Long categoryId) {
    log.info("根据分类id查询套餐：{}", categoryId);
    Setmeal setmeal = new Setmeal();
    setmeal.setCategoryId(categoryId);
    setmeal.setStatus(StatusConstant.ENABLE);

    List<Setmeal> list = setmealService.list(setmeal);
    return Result.success(list);
  }

  /**
   * Get dish list by setmeal ID.
   *
   * @param id The setmeal ID.
   * @return The list of dishes included in the setmeal.
   */
  @GetMapping("/dish/{id}")
  @Operation(summary = "根据套餐id查询包含的菜品列表")
  public Result<List<DishItemVo>> dishList(@PathVariable Long id) {
    List<DishItemVo> list = setmealService.getDishItemById(id);
    return Result.success(list);
  }
}
