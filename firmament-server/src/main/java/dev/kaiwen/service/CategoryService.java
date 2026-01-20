package dev.kaiwen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import dev.kaiwen.dto.CategoryDto;
import dev.kaiwen.dto.CategoryPageQueryDto;
import dev.kaiwen.entity.Category;
import dev.kaiwen.result.PageResult;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 分类服务接口.
 */
public interface CategoryService extends IService<Category> {

  /**
   * 新增分类.
   *
   * @param categoryDto 分类DTO
   */
  void save(CategoryDto categoryDto);

  /**
   * 分页查询.
   *
   * @param categoryPageQueryDto 分类分页查询DTO
   * @return 分页结果
   */
  PageResult pageQuery(CategoryPageQueryDto categoryPageQueryDto);

  /**
   * 根据id删除分类.
   *
   * @param id 分类ID
   */
  void deleteById(Long id);

  /**
   * 修改分类.
   *
   * @param categoryDto 分类DTO
   */
  void update(CategoryDto categoryDto);

  /**
   * 启用、禁用分类.
   *
   * @param status 状态
   * @param id     分类ID
   */
  void enableOrDisable(Integer status, Long id);

  /**
   * 根据类型查询分类.
   *
   * @param type 类型
   * @return 分类列表
   */
  List<Category> list(Integer type);

  /**
   * 根据分类ID集合批量查询分类并转换为Map.
   *
   * @param categoryIds 分类ID集合
   * @return 分类Map，key为分类ID，value为分类名称
   */
  Map<Long, String> getCategoryMapByIds(Set<Long> categoryIds);
}

