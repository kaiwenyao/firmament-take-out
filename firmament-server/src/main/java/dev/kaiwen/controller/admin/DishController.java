package dev.kaiwen.controller.admin;

import dev.kaiwen.dto.DishDTO;
import dev.kaiwen.dto.DishPageQueryDTO;
import dev.kaiwen.entity.Dish;
import dev.kaiwen.result.PageResult;
import dev.kaiwen.result.Result;
import dev.kaiwen.service.IDishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜品管理
 */
@RestController
@RequestMapping("/admin/dish")
@Tag(name = "菜品相关")
@Slf4j
@RequiredArgsConstructor
public class DishController {
    private final IDishService dishService;

    @PostMapping
    @Operation(summary = "新增菜品")
    public Result createDish(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品: {}", dishDTO);
        dishService.saveWithFlavor(dishDTO);
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
        dishService.deleteDish(ids);

        return Result.success();
    }
}

