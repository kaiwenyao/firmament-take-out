package dev.kaiwen.controller.admin;

import dev.kaiwen.constant.CacheConstant;
import dev.kaiwen.dto.DishDto;
import dev.kaiwen.dto.DishPageQueryDto;
import dev.kaiwen.result.PageResult;
import dev.kaiwen.result.Result;
import dev.kaiwen.service.DishService;
import dev.kaiwen.vo.DishVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dish management controller.
 */
@RestController
@RequestMapping("/admin/dish")
@Tag(name = "菜品相关")
@Slf4j
@RequiredArgsConstructor
public class DishController {

  private final DishService dishService;
  private final RedisTemplate<String, Object> redisTemplate;

  /**
   * Create a new dish.
   *
   * @param dishDto The dish data transfer object containing dish basic information and flavor
   *                information.
   * @return The operation result, returns success message on success.
   */
  @PostMapping
  @Operation(summary = "新增菜品")
  public Result<String> createDish(@RequestBody DishDto dishDto) {
    log.info("新增菜品: {}", dishDto);
    dishService.saveWithFlavor(dishDto);
    String key = CacheConstant.DISH_KEY_PREFIX + dishDto.getCategoryId();
    cleanCache(key);

    return Result.success();
  }

  /**
   * Page query for dishes.
   *
   * @param dishPageQueryDto The dish page query conditions, including page number, page size, dish
   *                         name, category ID, status and other query parameters.
   * @return The page query result containing dish list and pagination information.
   */
  @GetMapping("/page")
  @Operation(summary = "菜品分页查询")
  public Result<PageResult> pageQuery(DishPageQueryDto dishPageQueryDto) {
    log.info("菜品分页查询 {}", dishPageQueryDto);
    PageResult pr = dishService.pageQuery(dishPageQueryDto);
    return Result.success(pr);
  }

  /**
   * Batch delete dishes.
   *
   * @param ids The list of dish IDs to be deleted.
   * @return The operation result, returns success message on success.
   */
  @DeleteMapping
  @Operation(summary = "删除菜品")
  public Result<String> deleteDish(@RequestParam List<Long> ids) {
    log.info("菜品批量删除 {}", ids);

    // 先查询要删除的菜品，获取所有涉及的分类ID
    Set<Long> categoryIds = new HashSet<>();
    for (Long id : ids) {
      DishVo dish = dishService.getDishById(id);
      if (dish != null) {
        categoryIds.add(dish.getCategoryId());
      }
    }

    // 执行删除
    dishService.deleteDish(ids);

    // 只清理相关分类的缓存，而不是清空所有分类
    for (Long categoryId : categoryIds) {
      cleanCache(CacheConstant.DISH_KEY_PREFIX + categoryId);
    }
    log.info("已清理 {} 个分类的菜品缓存", categoryIds.size());

    return Result.success();
  }

  /**
   * Get dish by ID, used for data echo in edit page.
   *
   * @param id The dish ID.
   * @return The dish detailed information containing dish basic information and flavor information.
   */
  @GetMapping("/{id}")
  @Operation(summary = "根据id查询菜品")
  public Result<DishVo> getDishById(@PathVariable Long id) {
    log.info("根据id查询菜品: {}", id);
    DishVo dishVO = dishService.getDishById(id);
    return Result.success(dishVO);
  }

  /**
   * Update dish.
   *
   * @param dishDto The dish data transfer object containing dish ID, basic information and flavor
   *                information.
   * @return The operation result, returns success message on success.
   */
  @PutMapping
  @Operation(summary = "修改菜品")
  public Result<String> updateDish(@RequestBody DishDto dishDto) {
    log.info("修改菜品 {}", dishDto);

    // 先查询旧的菜品信息，获取旧分类ID
    DishVo oldDish = dishService.getDishById(dishDto.getId());

    // 执行更新
    dishService.updateDish(dishDto);

    // 清理新分类的缓存
    cleanCache(CacheConstant.DISH_KEY_PREFIX + dishDto.getCategoryId());

    // 如果分类发生了变更，也要清理旧分类的缓存
    if (oldDish != null && !oldDish.getCategoryId().equals(dishDto.getCategoryId())) {
      cleanCache(CacheConstant.DISH_KEY_PREFIX + oldDish.getCategoryId());
      log.info("菜品分类已变更，清理了旧分类 {} 和新分类 {} 的缓存",
          oldDish.getCategoryId(), dishDto.getCategoryId());
    } else {
      log.info("已清理分类 {} 的菜品缓存", dishDto.getCategoryId());
    }

    return Result.success();
  }

  /**
   * Start or stop dish sale.
   *
   * @param status The dish status, 1 means start sale, 0 means stop sale.
   * @param id     The dish ID.
   * @return The operation result, returns success message on success.
   */
  @PostMapping("/status/{status}")
  @Operation(summary = "菜品起售停售")
  public Result<String> startOrStop(@PathVariable Integer status, @RequestParam Long id) {
    log.info("菜品起售停售，状态：{}，菜品ID：{}", status, id);

    // 先查询菜品信息，获取分类ID
    DishVo dish = dishService.getDishById(id);

    // 执行起售停售操作
    dishService.startOrStop(status, id);

    // 只清理该菜品所属分类的缓存
    if (dish != null) {
      cleanCache(CacheConstant.DISH_KEY_PREFIX + dish.getCategoryId());
      log.info("已清理分类 {} 的菜品缓存", dish.getCategoryId());
    }

    return Result.success();
  }

  /**
   * Clean cache data.
   *
   * @param pattern The cache key pattern to match.
   */
  private void cleanCache(String pattern) {
    Set<String> keys = redisTemplate.keys(pattern);
    if (keys != null && !keys.isEmpty()) {
      redisTemplate.delete(keys);
    }
  }

}

