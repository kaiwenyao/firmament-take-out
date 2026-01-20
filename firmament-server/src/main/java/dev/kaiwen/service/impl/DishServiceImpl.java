package dev.kaiwen.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import dev.kaiwen.constant.MessageConstant;
import dev.kaiwen.constant.StatusConstant;
import dev.kaiwen.context.BaseContext;
import dev.kaiwen.converter.DishConverter;
import dev.kaiwen.dto.DishDto;
import dev.kaiwen.dto.DishPageQueryDto;
import dev.kaiwen.entity.Dish;
import dev.kaiwen.entity.DishFlavor;
import dev.kaiwen.entity.SetmealDish;
import dev.kaiwen.exception.DeletionNotAllowedException;
import dev.kaiwen.exception.DishDisableFailedException;
import dev.kaiwen.mapper.DishMapper;
import dev.kaiwen.result.PageResult;
import dev.kaiwen.service.CategoryService;
import dev.kaiwen.service.DishFlavorService;
import dev.kaiwen.service.DishService;
import dev.kaiwen.service.DishSetmealRelationService;
import dev.kaiwen.service.SetmealDishService;
import dev.kaiwen.vo.DishVo;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 菜品服务实现类.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

  private final DishFlavorService dishFlavorService;
  private final CategoryService categoryService;
  private final SetmealDishService setmealDishService;
  private final DishSetmealRelationService dishSetmealRelationService;

  @Override
  @Transactional
  public void saveWithFlavor(DishDto dishDto) {
    // 向菜品表插入数据
    Dish dish = DishConverter.INSTANCE.d2e(dishDto);
    this.save(dish);
    Long dishId = dish.getId();
    List<DishFlavor> flavors = dishDto.getFlavors();
    if (flavors != null && !flavors.isEmpty()) {
      flavors.forEach(f -> f.setDishId(dishId));
      dishFlavorService.saveBatch(flavors);
    }

  }

  @Override
  public PageResult pageQuery(DishPageQueryDto dishPageQueryDto) {
    Page<Dish> pageInfo = new Page<>(dishPageQueryDto.getPage(), dishPageQueryDto.getPageSize());

    lambdaQuery() // 开启链式查询，底层创建了 LambdaQueryChainWrapper
        // ▼ name: 模糊查询 (like)
        // 第一个参数是 boolean：只有当 name 有值得时候，才会拼接这条 SQL
        .like(StringUtils.hasText(dishPageQueryDto.getName()),
            Dish::getName, dishPageQueryDto.getName())

        // ▼ categoryId: 精确查询 (eq)
        // 只有当 categoryId 不为 null 时才拼接
        .eq(dishPageQueryDto.getCategoryId() != null,
            Dish::getCategoryId, dishPageQueryDto.getCategoryId())

        // ▼ status: 精确查询 (eq)
        // 只有当 status 不为 null 时才拼接
        .eq(dishPageQueryDto.getStatus() != null,
            Dish::getStatus, dishPageQueryDto.getStatus())
        .orderByDesc(Dish::getCreateTime)
        .page(pageInfo);

    // ================== 分割线：下面是 Entity 转 VO 的过程 ==================

    // 取出原始数据并转换为VO，填充分类名称
    List<Dish> records = pageInfo.getRecords();
    List<DishVo> voList = convertToVoWithCategoryName(records);

    return new PageResult(pageInfo.getTotal(), voList);
  }

  @Override
  @Transactional
  public void deleteDish(List<Long> ids) {
    boolean exists = lambdaQuery()
        .in(Dish::getId, ids)
        .eq(Dish::getStatus, StatusConstant.ENABLE)
        .exists(); // 生成 SQL: SELECT 1 FROM dish WHERE ... LIMIT 1

    if (exists) {
      throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
    }
    // 2. 检查是否被套餐关联 (使用 exists 优化)
    // 语义：去 setmeal_dish 表查看，只要 ids 中有任何一个出现在 dish_id 列中，就返回 true
    boolean isRelated = setmealDishService.lambdaQuery()
        .in(SetmealDish::getDishId, ids)
        .exists();

    if (isRelated) {
      // 存在关联数据，抛出异常
      throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
    }
    // 可以删除
    this.removeByIds(ids);
    // 删除关联的口味数据 (dish_flavor 表)
    // 语义：DELETE FROM dish_flavor WHERE dish_id IN (1, 2, 3)
    dishFlavorService.lambdaUpdate() // 开启链式更新/删除
        .in(DishFlavor::getDishId, ids) // 指定条件：dish_id 在 ids 列表中
        .remove(); // 执行删除操作

  }

  @Override
  public DishVo getDishById(Long id) {
    Dish dish = this.getById(id);
    DishVo dishVo = DishConverter.INSTANCE.e2v(dish);
    List<DishFlavor> dishFlavors = dishFlavorService.lambdaQuery()
        .in(DishFlavor::getDishId, id)
        .list();
    dishVo.setFlavors(dishFlavors);
    return dishVo;
  }

  @Override
  public void updateDish(DishDto dishDto) {
    // 基本信息
    Dish dish = DishConverter.INSTANCE.d2e(dishDto);
    this.updateById(dish);
    // 口味先删除再插入
    dishFlavorService.lambdaUpdate()
        .in(DishFlavor::getDishId, dish.getId())
        .remove();
    List<DishFlavor> flavors = dishDto.getFlavors();
    // 判空保护：只有前端真的传了口味，我们才执行插入
    if (flavors != null && !flavors.isEmpty()) {
      // ⚠️ 核心步骤：关联外键
      // 前端传来的 flavor 对象里通常没有 dishId，必须手动把当前菜品的 ID 赋给它们
      flavors.forEach(flavor -> flavor.setDishId(dish.getId()));
      // 批量插入数据库
      // 对应 SQL: INSERT INTO dish_flavor (dish_id, name, value) VALUES (?,?,?), (?,?,?)...
      dishFlavorService.saveBatch(flavors);
    }

  }

  @Override
  public List<DishVo> listWithFlavor(Dish dish) {

    // 1. 构造查询条件并查询菜品列表
    List<Dish> dishList = this.lambdaQuery()
        .eq(dish.getCategoryId() != null, Dish::getCategoryId,
            dish.getCategoryId()) // 动态拼接 categoryId
        .eq(dish.getStatus() != null, Dish::getStatus, dish.getStatus()) // 动态拼接status(比如只查起售的)
        .list();
    // 如果没查到菜品，直接返回空集合
    if (dishList == null || dishList.isEmpty()) {
      return Collections.emptyList();
    }

    // 2. 将 Dish 转为 DishVO
    List<DishVo> dishVoList = dishList.stream().map(DishConverter.INSTANCE::e2v)
        .toList();

    // 3. 批量查询并填充分味数据
    fillFlavorsToVoList(dishVoList, dishList);

    return dishVoList;
  }

  /**
   * 菜品起售、停售.
   *
   * @param status 状态
   * @param id     菜品ID
   */
  @Override
  public void startOrStop(Integer status, Long id) {
    // 停售菜品时，判断是否有起售的套餐在使用这个菜品，有起售套餐提示"菜品关联了起售中的套餐，无法停售"
    // 使用关系检查服务，避免循环依赖
    if (StatusConstant.DISABLE.equals(status)
        && dishSetmealRelationService.hasEnabledSetmealUsingDish(id)) {
      throw new DishDisableFailedException(MessageConstant.DISH_DISABLE_FAILED);
    }

    // 5. 使用 MyBatis Plus 的链式更新方法更新菜品状态
    lambdaUpdate()
        .eq(Dish::getId, id)
        .set(Dish::getStatus, status)
        .set(Dish::getUpdateTime, LocalDateTime.now())
        .set(Dish::getUpdateUser, BaseContext.getCurrentId())
        .update();
  }

  /**
   * 将菜品列表转换为VO列表，并填充分类名称.
   *
   * @param dishes 菜品列表
   * @return VO列表
   */
  private List<DishVo> convertToVoWithCategoryName(List<Dish> dishes) {
    // 批量查询分类并转换为Map
    Map<Long, String> categoryMap = getCategoryMap(dishes);

    // 组装 VO 并填充分类名称
    return dishes.stream().map(dish -> {
      DishVo dishVo = DishConverter.INSTANCE.e2v(dish);
      // 从 Map 中直接取名字，不再查库
      String categoryName = categoryMap.get(dish.getCategoryId());
      dishVo.setCategoryName(categoryName);
      return dishVo;
    }).toList();
  }

  /**
   * 批量查询分类并转换为Map.
   *
   * @param dishes 菜品列表
   * @return 分类Map，key为分类ID，value为分类名称
   */
  private Map<Long, String> getCategoryMap(List<Dish> dishes) {
    // 提取所有的分类 ID (去重)
    Set<Long> categoryIds = dishes.stream()
        .map(Dish::getCategoryId)
        .filter(Objects::nonNull) // 过滤掉没有分类ID的数据，防止报错
        .collect(Collectors.toSet());

    // 使用 CategoryService 的公共方法批量查询分类并转换为Map
    return categoryService.getCategoryMapByIds(categoryIds);
  }

  /**
   * 批量查询口味并填充到VO列表.
   *
   * @param dishVoList VO列表
   * @param dishList   菜品列表
   */
  private void fillFlavorsToVoList(List<DishVo> dishVoList, List<Dish> dishList) {
    // 提取所有菜品的 ID 集合
    List<Long> dishIds = dishList.stream()
        .map(Dish::getId)
        .toList();

    // 一次性查询这些菜品对应的所有口味
    List<DishFlavor> allFlavors = dishFlavorService.lambdaQuery()
        .in(DishFlavor::getDishId, dishIds)
        .list();

    // 在内存中将口味按 dishId 分组
    Map<Long, List<DishFlavor>> flavorMap = allFlavors.stream()
        .collect(Collectors.groupingBy(DishFlavor::getDishId));

    // 遍历 VO 列表，从 Map 中直接取值填充，不再查库
    dishVoList.forEach(vo -> vo.setFlavors(flavorMap.get(vo.getId())));
  }
}
