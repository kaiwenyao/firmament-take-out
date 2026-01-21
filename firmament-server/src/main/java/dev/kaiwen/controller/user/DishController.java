package dev.kaiwen.controller.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kaiwen.constant.StatusConstant;
import dev.kaiwen.entity.Dish;
import dev.kaiwen.result.Result;
import dev.kaiwen.service.DishService;
import dev.kaiwen.vo.DishVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Collections;
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
  private final RedisTemplate<String, String> redisTemplateStringString;
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
    String cacheJson = redisTemplateStringString.opsForValue().get(key);
    if (cacheJson != null && !cacheJson.isBlank()) {
      try {
        List<DishVo> list = parseDishCache(cacheJson);
        if (list != null && !list.isEmpty()) {
          log.info("菜品缓存命中，key={}", key);
          return Result.success(list);
        }
      } catch (JsonProcessingException | IllegalArgumentException ex) {
        log.warn("菜品缓存解析失败，已清理缓存 key={}", key, ex);
        redisTemplateStringString.delete(key);
      }
    }

    // 缓存未命中，从数据库查询
    Dish dish = new Dish();
    dish.setCategoryId(categoryId);
    dish.setStatus(StatusConstant.ENABLE);
    List<DishVo> list = dishService.listWithFlavor(dish);
    if (list != null) {
      try {
        redisTemplateStringString.opsForValue().set(key, objectMapper.writeValueAsString(list));
      } catch (JsonProcessingException ex) {
        log.warn("菜品缓存序列化失败，跳过缓存 key={}", key, ex);
      }
    }
    return Result.success(list);
  }

  private List<DishVo> parseDishCache(String cacheJson) throws JsonProcessingException {
    Object raw = objectMapper.readValue(cacheJson, Object.class);
    if (!(raw instanceof List<?> rawList)) {
      return Collections.emptyList();
    }
    Object payload = rawList;
    // 兼容带类型信息的 wrapper array: ["java.util.ArrayList", [ ... ]]
    if (rawList.size() == 2 && rawList.get(0) instanceof String
        && rawList.get(1) instanceof List) {
      payload = rawList.get(1);
    }
    return objectMapper.convertValue(payload, new TypeReference<>() {
    });
  }

}
