package dev.kaiwen.controller.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kaiwen.constant.StatusConstant;
import dev.kaiwen.entity.Dish;
import dev.kaiwen.result.Result;
import dev.kaiwen.service.DishService;
import dev.kaiwen.vo.DishVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dish controller for client side.
 */
@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Tag(name = "C端-菜品浏览接口")
@RequiredArgsConstructor
public class DishController {

  private final DishService dishService;
  private final RedisTemplate<String, Object> redisTemplateStringObject;
  private final ObjectMapper objectMapper;

  /**
   * Get dishes by category ID.
   *
   * @param categoryId The category ID.
   * @return The list of dishes in the category.
   */
  @GetMapping("/list")
  @Operation(summary = "根据分类id查询菜品")
  public Result<List<DishVo>> list(@RequestParam Long categoryId) {
    log.info("根据分类id查询菜品：{}", categoryId);
    String key = "dish_" + categoryId;

    // 从 Redis 获取缓存数据
    Object cacheData = redisTemplateStringObject.opsForValue().get(key);
    if (cacheData != null) {
      // ✅ 使用 ObjectMapper 安全地把 Object 转成 List<DishVo>
      // 这行代码会自动处理 LinkedHashMap 到 DishVo 的映射，非常安全
      List<DishVo> list = objectMapper.convertValue(cacheData, new TypeReference<>() {
      });
      if (!list.isEmpty()) {
        return Result.success(list);
      }
    }

    // 缓存未命中，从数据库查询
    Dish dish = new Dish();
    dish.setCategoryId(categoryId);
    dish.setStatus(StatusConstant.ENABLE);
    List<DishVo> list = dishService.listWithFlavor(dish);
    if (list != null) {
      redisTemplateStringObject.opsForValue().set(key, list);
    }
    return Result.success(list);
  }

}
