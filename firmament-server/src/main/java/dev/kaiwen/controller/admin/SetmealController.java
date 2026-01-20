package dev.kaiwen.controller.admin;

import dev.kaiwen.dto.SetmealDto;
import dev.kaiwen.dto.SetmealPageQueryDto;
import dev.kaiwen.result.PageResult;
import dev.kaiwen.result.Result;
import dev.kaiwen.service.SetmealService;
import dev.kaiwen.vo.SetmealVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
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
 * Setmeal management controller.
 */
@RestController
@RequestMapping("/admin/setmeal")
@Tag(name = "套餐相关接口")
@Slf4j
@RequiredArgsConstructor
public class SetmealController {

  private final SetmealService setmealService;

  /**
   * Create a new setmeal.
   *
   * @param setmealDto The setmeal data transfer object containing setmeal basic information and
   *                   associated dish information.
   * @return The operation result, returns success message on success.
   */
  @PostMapping
  @Operation(summary = "新增套餐")
  @CacheEvict(cacheNames = "setmealCache", key = "#setmealDto.categoryId")
  public Result<String> save(@RequestBody SetmealDto setmealDto) {

    log.info("新增套餐：{}", setmealDto);
    setmealService.saveWithDish(setmealDto);
    return Result.success();
  }

  /**
   * Page query for setmeals.
   *
   * @param setmealPageQueryDto The setmeal page query conditions, including page number, page size,
   *                            setmeal name, category ID, status and other query parameters.
   * @return The page query result containing setmeal list and pagination information.
   */
  @GetMapping("/page")
  @Operation(summary = "套餐分页查询")
  public Result<PageResult> page(SetmealPageQueryDto setmealPageQueryDto) {
    log.info("套餐分页查询，参数：{}", setmealPageQueryDto);
    PageResult pageResult = setmealService.pageQuery(setmealPageQueryDto);
    return Result.success(pageResult);
  }

  /**
   * Get setmeal by ID, used for data echo in edit page.
   *
   * @param id The setmeal ID.
   * @return The setmeal detailed information containing setmeal basic information and associated
   *     dish information.
   */
  @GetMapping("/{id}")
  @Operation(summary = "根据id查询套餐")
  public Result<SetmealVO> getById(@PathVariable Long id) {
    log.info("根据id查询套餐：{}", id);
    SetmealVO setmealVO = setmealService.getByIdWithDish(id);
    return Result.success(setmealVO);
  }

  /**
   * Update setmeal.
   *
   * @param setmealDto The setmeal data transfer object containing setmeal ID, basic information and
   *                   associated dish information.
   * @return The operation result, returns success message on success.
   */
  @PutMapping
  @Operation(summary = "修改套餐")
  @CacheEvict(cacheNames = "setmealCache", allEntries = true)
  public Result<String> update(@RequestBody SetmealDto setmealDto) {
    log.info("修改套餐：{}", setmealDto);
    setmealService.update(setmealDto);
    return Result.success();
  }

  /**
   * Batch delete setmeals.
   *
   * @param ids The list of setmeal IDs to be deleted.
   * @return The operation result, returns success message on success.
   */
  @DeleteMapping
  @Operation(summary = "批量删除套餐")
  @CacheEvict(cacheNames = "setmealCache", allEntries = true)
  public Result<String> delete(@RequestParam List<Long> ids) {
    log.info("批量删除套餐：{}", ids);
    setmealService.deleteBatch(ids);
    return Result.success();
  }

  /**
   * Start or stop setmeal sale.
   *
   * @param status The setmeal status, 1 means start sale, 0 means stop sale.
   * @param id     The setmeal ID.
   * @return The operation result, returns success message on success.
   */
  @PostMapping("/status/{status}")
  @Operation(summary = "套餐起售停售")
  @CacheEvict(cacheNames = "setmealCache", allEntries = true)
  public Result<String> startOrStop(@PathVariable Integer status, @RequestParam Long id) {
    log.info("套餐起售停售，状态：{}，套餐ID：{}", status, id);
    setmealService.startOrStop(status, id);
    return Result.success();
  }
}