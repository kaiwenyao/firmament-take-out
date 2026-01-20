package dev.kaiwen.controller.admin;

import dev.kaiwen.dto.CategoryDto;
import dev.kaiwen.dto.CategoryPageQueryDto;
import dev.kaiwen.entity.Category;
import dev.kaiwen.result.PageResult;
import dev.kaiwen.result.Result;
import dev.kaiwen.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Category management controller.
 */
@RestController
@RequestMapping("/admin/category")
@Tag(name = "分类相关接口")
@Slf4j
@RequiredArgsConstructor
public class CategoryController {

  private final CategoryService categoryService;

  /**
   * Create a new category.
   *
   * @param categoryDto The category data transfer object containing category information.
   * @return The operation result, returns success message on success.
   */
  @PostMapping
  @Operation(summary = "新增分类")
  public Result<String> save(@RequestBody CategoryDto categoryDto) {
    log.info("新增分类：{}", categoryDto);
    categoryService.save(categoryDto);
    return Result.success();
  }

  /**
   * Page query for categories.
   *
   * @param categoryPageQueryDto The category page query conditions, including page number, page
   *                             size, category name, type and other query parameters.
   * @return The page query result containing category list and pagination information.
   */
  @GetMapping("/page")
  @Operation(summary = "分类分页查询")
  public Result<PageResult> page(CategoryPageQueryDto categoryPageQueryDto) {
    log.info("分页查询：{}", categoryPageQueryDto);
    PageResult pageResult = categoryService.pageQuery(categoryPageQueryDto);
    return Result.success(pageResult);
  }

  /**
   * Delete category by ID.
   *
   * @param id The category ID.
   * @return The operation result, returns success message on success.
   */
  @DeleteMapping
  @Operation(summary = "删除分类")
  public Result<String> deleteById(Long id) {
    log.info("删除分类：{}", id);
    categoryService.deleteById(id);
    return Result.success();
  }

  /**
   * Update category.
   *
   * @param categoryDto The category data transfer object containing category ID and updated
   *                    information.
   * @return The operation result, returns success message on success.
   */
  @PutMapping
  @Operation(summary = "修改分类")
  public Result<String> update(@RequestBody CategoryDto categoryDto) {
    categoryService.update(categoryDto);
    return Result.success();
  }

  /**
   * Enable or disable category.
   *
   * @param status The category status, 1 means enabled, 0 means disabled.
   * @param id     The category ID.
   * @return The operation result, returns success message on success.
   */
  @PostMapping("/status/{status}")
  @Operation(summary = "启用禁用分类")
  public Result<String> enableOrDisable(@PathVariable Integer status, Long id) {
    categoryService.enableOrDisable(status, id);
    return Result.success();
  }

  /**
   * Get categories by type.
   *
   * @param type The category type.
   * @return The list of categories matching the type.
   */
  @GetMapping("/list")
  @Operation(summary = "根据类型查询分类")
  public Result<List<Category>> list(Integer type) {
    List<Category> list = categoryService.list(type);
    return Result.success(list);
  }
}
