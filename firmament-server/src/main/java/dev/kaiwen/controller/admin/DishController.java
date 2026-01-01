package dev.kaiwen.controller.admin;

import dev.kaiwen.dto.DishDTO;
import dev.kaiwen.dto.DishPageQueryDTO;
import dev.kaiwen.entity.Dish;
import dev.kaiwen.result.PageResult;
import dev.kaiwen.result.Result;
import dev.kaiwen.service.DishService;
import dev.kaiwen.vo.DishVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 菜品管理
 */
@RestController
@RequestMapping("/admin/dish")
@Tag(name = "菜品相关")
@Slf4j
@RequiredArgsConstructor
public class DishController {
    private final DishService dishService;
    private final RedisTemplate redisTemplate;

    @PostMapping
    @Operation(summary = "新增菜品")
    public Result createDish(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品: {}", dishDTO);
        dishService.saveWithFlavor(dishDTO);
        String key = "dish_"  + dishDTO.getCategoryId();
        cleanCache(key);

        return Result.success();
    }

    @GetMapping("/page")
    @Operation(summary = "菜品分页查询")
    public Result<PageResult> pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        log.info("菜品分页查询 {}", dishPageQueryDTO);
        PageResult pr = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pr);
    }

    @DeleteMapping
    @Operation(summary = "删除菜品")
    public Result deleteDish(@RequestParam List<Long> ids) {
        log.info("菜品批量删除 {}", ids);

        // 先查询要删除的菜品，获取所有涉及的分类ID
        Set<Long> categoryIds = new HashSet<>();
        for (Long id : ids) {
            DishVO dish = dishService.getDishById(id);
            if (dish != null) {
                categoryIds.add(dish.getCategoryId());
            }
        }

        // 执行删除
        dishService.deleteDish(ids);

        // 只清理相关分类的缓存，而不是清空所有分类
        for (Long categoryId : categoryIds) {
            cleanCache("dish_" + categoryId);
        }
        log.info("已清理 {} 个分类的菜品缓存", categoryIds.size());

        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据id查询菜品")
    public Result<DishVO> getDishById(@PathVariable Long id) {
        log.info("根据id查询菜品: {}", id);
        DishVO dishVO = dishService.getDishById(id);
        return Result.success(dishVO);
    }

    @PutMapping
    @Operation(summary = "修改菜品")
    public Result updateDish(@RequestBody DishDTO dishDTO) {
        log.info("修改菜品 {}", dishDTO);

        // 先查询旧的菜品信息，获取旧分类ID
        DishVO oldDish = dishService.getDishById(dishDTO.getId());

        // 执行更新
        dishService.updateDish(dishDTO);

        // 清理新分类的缓存
        cleanCache("dish_" + dishDTO.getCategoryId());

        // 如果分类发生了变更，也要清理旧分类的缓存
        if (oldDish != null && !oldDish.getCategoryId().equals(dishDTO.getCategoryId())) {
            cleanCache("dish_" + oldDish.getCategoryId());
            log.info("菜品分类已变更，清理了旧分类 {} 和新分类 {} 的缓存",
                    oldDish.getCategoryId(), dishDTO.getCategoryId());
        } else {
            log.info("已清理分类 {} 的菜品缓存", dishDTO.getCategoryId());
        }

        return Result.success();
    }

    /**
     * 菜品起售停售
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @Operation(summary = "菜品起售停售")
    public Result<String> startOrStop(@PathVariable Integer status, @RequestParam Long id) {
        log.info("菜品起售停售，状态：{}，菜品ID：{}", status, id);

        // 先查询菜品信息，获取分类ID
        DishVO dish = dishService.getDishById(id);

        // 执行起售停售操作
        dishService.startOrStop(status, id);

        // 只清理该菜品所属分类的缓存
        if (dish != null) {
            cleanCache("dish_" + dish.getCategoryId());
            log.info("已清理分类 {} 的菜品缓存", dish.getCategoryId());
        }

        return Result.success();
    }

    /**
     * 清理缓存数据
     * @param pattern
     */

    private void cleanCache(String pattern) {
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }

}

