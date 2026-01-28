package dev.kaiwen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import dev.kaiwen.constant.MessageConstant;
import dev.kaiwen.constant.StatusConstant;
import dev.kaiwen.context.BaseContext;
import dev.kaiwen.converter.CategoryConverter;
import dev.kaiwen.dto.CategoryDto;
import dev.kaiwen.dto.CategoryPageQueryDto;
import dev.kaiwen.entity.Category;
import dev.kaiwen.entity.Dish;
import dev.kaiwen.entity.Setmeal;
import dev.kaiwen.exception.DeletionNotAllowedException;
import dev.kaiwen.mapper.CategoryMapper;
import dev.kaiwen.mapper.DishMapper;
import dev.kaiwen.mapper.SetmealMapper;
import dev.kaiwen.result.PageResult;
import dev.kaiwen.service.CategoryService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 分类业务层.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements
    CategoryService {

  private final CategoryMapper mapper;
  private final DishMapper dishMapper;
  private final SetmealMapper setmealMapper;

  /**
   * 新增分类.
   *
   * @param categoryDto 分类DTO
   */
  @Override
  public void save(CategoryDto categoryDto) {
    // 使用 MapStruct 进行对象转换
    Category category = CategoryConverter.INSTANCE.d2e(categoryDto);

    // 分类状态默认为禁用状态0
    category.setStatus(StatusConstant.DISABLE);

    // 使用 ServiceImpl 提供的 save 方法
    // 注意：createTime、updateTime、createUser、updateUser 会通过 AutoFillMetaObjectHandler 自动填充
    this.save(category);
  }

  /**
   * 分页查询.
   *
   * @param categoryPageQueryDto 分类分页查询DTO
   * @return 分页结果
   */
  @Override
  public PageResult pageQuery(CategoryPageQueryDto categoryPageQueryDto) {
    // 使用 MyBatis Plus 分页插件
    Page<Category> page = new Page<>(categoryPageQueryDto.getPage(),
        categoryPageQueryDto.getPageSize());

    // 使用 Wrappers + mapper 方式查询
    LambdaQueryWrapper<Category> wrapper = Wrappers.lambdaQuery(Category.class)
        .like(StringUtils.hasText(categoryPageQueryDto.getName()), Category::getName,
            categoryPageQueryDto.getName())
        .eq(categoryPageQueryDto.getType() != null, Category::getType,
            categoryPageQueryDto.getType())
        .orderByAsc(Category::getSort)
        .orderByDesc(Category::getCreateTime);
    mapper.selectPage(page, wrapper);

    // 直接从 page 对象中获取填充好的数据
    return new PageResult(page.getTotal(), page.getRecords());
  }

  /**
   * 根据id删除分类.
   *
   * @param id 分类ID
   */
  @Override
  public void deleteById(Long id) {
    // 查询当前分类是否关联了菜品，如果关联了就抛出业务异常
    // 使用 Wrappers + mapper 方式查询
    LambdaQueryWrapper<Dish> dishWrapper = Wrappers.lambdaQuery(Dish.class)
        .eq(Dish::getCategoryId, id);
    boolean dishExists = dishMapper.selectCount(dishWrapper) > 0;
    if (dishExists) {
      // 当前分类下有菜品，不能删除
      throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_DISH);
    }

    // 查询当前分类是否关联了套餐，如果关联了就抛出业务异常
    LambdaQueryWrapper<Setmeal> setmealWrapper = Wrappers.lambdaQuery(Setmeal.class)
        .eq(Setmeal::getCategoryId, id);
    boolean setmealExists = setmealMapper.selectCount(setmealWrapper) > 0;
    if (setmealExists) {
      // 当前分类下有套餐，不能删除
      throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL);
    }

    // 删除分类数据，使用 ServiceImpl 提供的 removeById 方法
    this.removeById(id);
  }

  /**
   * 修改分类.
   *
   * @param categoryDto 分类DTO
   */
  @Override
  public void update(CategoryDto categoryDto) {
    // 使用 MapStruct 进行对象转换
    Category category = CategoryConverter.INSTANCE.d2e(categoryDto);

    // 使用 ServiceImpl 提供的 updateById 方法，只更新非空字段
    // 注意：updateTime、updateUser 会通过 AutoFillMetaObjectHandler 自动填充
    this.updateById(category);
  }

  /**
   * 启用、禁用分类.
   *
   * @param status 状态
   * @param id     分类ID
   */
  @Override
  public void enableOrDisable(Integer status, Long id) {
    // 使用 Wrappers + mapper 方式更新
    LambdaUpdateWrapper<Category> updateWrapper = Wrappers.lambdaUpdate(Category.class)
        .eq(Category::getId, id)
        .set(Category::getStatus, status)
        .set(Category::getUpdateTime, LocalDateTime.now())
        .set(Category::getUpdateUser, BaseContext.getCurrentId());
    mapper.update(null, updateWrapper);
  }

  /**
   * 根据类型查询分类.
   *
   * @param type 类型
   * @return 分类列表
   */
  @Override
  public List<Category> list(Integer type) {
    // 使用 Wrappers + mapper 方式查询
    LambdaQueryWrapper<Category> wrapper = Wrappers.lambdaQuery(Category.class)
        .eq(Category::getStatus, StatusConstant.ENABLE)
        .eq(type != null, Category::getType, type)
        .orderByAsc(Category::getSort)
        .orderByDesc(Category::getCreateTime);
    return mapper.selectList(wrapper);
  }

  @Override
  public Map<Long, String> getCategoryMapByIds(Set<Long> categoryIds) {
    // 批量查询分类并转换为Map
    Map<Long, String> categoryMap = new HashMap<>();
    if (categoryIds != null && !categoryIds.isEmpty()) {
      List<Category> categories = this.listByIds(categoryIds);
      // 将 List 转为 Map<ID, Name>，方便后面 O(1) 级别的快速查找
      categoryMap = categories.stream()
          .collect(Collectors.toMap(Category::getId, Category::getName));
    }
    return categoryMap;
  }
}
