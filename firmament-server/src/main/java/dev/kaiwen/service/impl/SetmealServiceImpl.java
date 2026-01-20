package dev.kaiwen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import dev.kaiwen.constant.MessageConstant;
import dev.kaiwen.constant.StatusConstant;
import dev.kaiwen.context.BaseContext;
import dev.kaiwen.converter.SetmealConverter;
import dev.kaiwen.dto.SetmealDto;
import dev.kaiwen.dto.SetmealPageQueryDto;
import dev.kaiwen.entity.Dish;
import dev.kaiwen.entity.Setmeal;
import dev.kaiwen.entity.SetmealDish;
import dev.kaiwen.exception.DeletionNotAllowedException;
import dev.kaiwen.exception.SetmealEnableFailedException;
import dev.kaiwen.mapper.DishMapper;
import dev.kaiwen.mapper.SetmealMapper;
import dev.kaiwen.result.PageResult;
import dev.kaiwen.service.CategoryService;
import dev.kaiwen.service.DishSetmealRelationService;
import dev.kaiwen.service.SetmealDishService;
import dev.kaiwen.service.SetmealService;
import dev.kaiwen.vo.DishItemVo;
import dev.kaiwen.vo.SetmealVo;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 套餐服务实现类. 提供套餐的增删改查、分页查询、起售停售等功能.
 */
@Service
@RequiredArgsConstructor
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements
    SetmealService {

  private final SetmealDishService setmealDishService;
  private final CategoryService categoryService;
  private final DishSetmealRelationService dishSetmealRelationService;
  private final DishMapper dishMapper;

  /**
   * 新增套餐，同时需要保存套餐和菜品的关联关系.
   *
   * @param setmealDto 套餐DTO
   */
  @Override
  @Transactional
  public void saveWithDish(SetmealDto setmealDto) {
    // 使用 MapStruct 进行对象转换
    Setmeal setmeal = SetmealConverter.INSTANCE.d2e(setmealDto);

    // 向套餐表插入数据，使用 MyBatis Plus 的 save 方法
    this.save(setmeal);

    // 获取生成的套餐id
    Long setmealId = setmeal.getId();

    // 保存套餐和菜品的关联关系
    List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
    if (setmealDishes != null && !setmealDishes.isEmpty()) {
      setmealDishes.forEach(setmealDish -> setmealDish.setSetmealId(setmealId));
      // 使用 MyBatis Plus 的批量保存方法
      setmealDishService.saveBatch(setmealDishes);
    }
  }

  /**
   * 条件查询.
   *
   * @param setmeal 套餐查询条件
   * @return 套餐列表
   */
  @Override
  public List<Setmeal> list(Setmeal setmeal) {
    // 使用 MyBatis Plus 的链式调用构建动态查询条件
    return lambdaQuery()
        .eq(setmeal.getCategoryId() != null, Setmeal::getCategoryId, setmeal.getCategoryId())
        .eq(setmeal.getStatus() != null, Setmeal::getStatus, setmeal.getStatus())
        .like(StringUtils.hasText(setmeal.getName()), Setmeal::getName, setmeal.getName())
        .orderByDesc(Setmeal::getCreateTime)
        .list();
  }

  /**
   * 根据id查询菜品选项.
   *
   * @param id 套餐ID
   * @return 菜品选项列表
   */
  @Override
  public List<DishItemVo> getDishItemById(Long id) {
    // 1. 查询套餐关联的菜品关系，获取 dishId 和 copies
    List<SetmealDish> setmealDishes = setmealDishService.lambdaQuery()
        .eq(SetmealDish::getSetmealId, id)
        .list();

    if (setmealDishes == null || setmealDishes.isEmpty()) {
      return Collections.emptyList();
    }

    // 2. 提取菜品ID列表
    List<Long> dishIds = setmealDishes.stream()
        .map(SetmealDish::getDishId)
        .filter(Objects::nonNull)
        .toList();

    if (dishIds.isEmpty()) {
      return Collections.emptyList();
    }

    // 3. 批量查询菜品信息（直接使用 Mapper，避免 Service 层循环依赖）
    List<Dish> dishList = dishMapper.selectList(
        new LambdaQueryWrapper<Dish>().in(Dish::getId, dishIds)
    );

    // 4. 构建 dishId -> SetmealDish 的映射，方便快速查找 copies
    Map<Long, SetmealDish> setmealDishMap = setmealDishes.stream()
        .collect(Collectors.toMap(SetmealDish::getDishId, sd -> sd,
            (existing, replacement) -> existing));

    // 5. 组装 DishItemVO
    return dishList.stream()
        .map(dish -> {
          SetmealDish setmealDish = setmealDishMap.get(dish.getId());
          return DishItemVo.builder()
              .name(dish.getName())
              .copies(setmealDish != null ? setmealDish.getCopies() : 1)
              .image(dish.getImage())
              .description(dish.getDescription())
              .build();
        })
        .toList();
  }

  /**
   * 分页查询.
   *
   * @param setmealPageQueryDto 套餐分页查询DTO
   * @return 分页结果
   */
  @Override
  public PageResult pageQuery(SetmealPageQueryDto setmealPageQueryDto) {
    // 使用 MyBatis Plus 分页插件
    Page<Setmeal> pageInfo = new Page<>(setmealPageQueryDto.getPage(),
        setmealPageQueryDto.getPageSize());

    // 使用链式调用构建查询条件并执行分页查询
    lambdaQuery()
        // name: 模糊查询 (like)
        .like(StringUtils.hasText(setmealPageQueryDto.getName()),
            Setmeal::getName, setmealPageQueryDto.getName())
        // categoryId: 精确查询 (eq)
        .eq(setmealPageQueryDto.getCategoryId() != null,
            Setmeal::getCategoryId, setmealPageQueryDto.getCategoryId())
        // status: 精确查询 (eq)
        .eq(setmealPageQueryDto.getStatus() != null,
            Setmeal::getStatus, setmealPageQueryDto.getStatus())
        .orderByDesc(Setmeal::getCreateTime)
        .page(pageInfo);

    // ================== 分割线：下面是 Entity 转 VO 的过程 ==================

    // 取出原始数据 List<Setmeal>
    List<Setmeal> records = pageInfo.getRecords();

    // 提取所有的分类 ID (去重)
    Set<Long> categoryIds = records.stream()
        .map(Setmeal::getCategoryId)
        .filter(Objects::nonNull) // 过滤掉没有分类ID的数据，防止报错
        .collect(Collectors.toSet());

    // 使用 CategoryService 的公共方法批量查询分类并转换为Map
    Map<Long, String> categoryMap = categoryService.getCategoryMapByIds(categoryIds);

    // 组装 VO
    List<SetmealVo> voList = records.stream().map(setmeal -> {
      // 属性拷贝
      SetmealVo setmealVo = SetmealConverter.INSTANCE.e2v(setmeal);

      // 从 Map 中直接取名字，不再查库
      String categoryName = categoryMap.get(setmeal.getCategoryId());
      setmealVo.setCategoryName(categoryName);

      return setmealVo;
    }).toList();

    return new PageResult(pageInfo.getTotal(), voList);
  }

  /**
   * 根据id查询套餐和套餐菜品关系.
   *
   * @param id 套餐ID
   * @return 套餐VO（包含套餐菜品关系）
   */
  @Override
  public SetmealVo getByIdWithDish(Long id) {
    // 使用 MyBatis Plus 查询套餐基本信息
    Setmeal setmeal = this.getById(id);

    // 使用 MapStruct 进行对象转换
    SetmealVo setmealVo = SetmealConverter.INSTANCE.e2v(setmeal);

    // 使用 MyBatis Plus 查询套餐和菜品的关联关系
    List<SetmealDish> setmealDishes = setmealDishService.lambdaQuery()
        .eq(SetmealDish::getSetmealId, id)
        .list();

    setmealVo.setSetmealDishes(setmealDishes);

    return setmealVo;
  }

  /**
   * 修改套餐.
   *
   * @param setmealDto 套餐DTO
   */
  @Override
  @Transactional
  public void update(SetmealDto setmealDto) {
    // 使用 MapStruct 进行对象转换
    Setmeal setmeal = SetmealConverter.INSTANCE.d2e(setmealDto);

    // 1. 修改套餐表，使用 MyBatis Plus 的 updateById 方法
    this.updateById(setmeal);

    // 套餐id
    Long setmealId = setmealDto.getId();

    // 2. 删除套餐和菜品的关联关系，使用 MyBatis Plus 的链式调用
    setmealDishService.lambdaUpdate()
        .eq(SetmealDish::getSetmealId, setmealId)
        .remove();

    // 3. 重新插入套餐和菜品的关联关系
    List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
    if (setmealDishes != null && !setmealDishes.isEmpty()) {
      setmealDishes.forEach(setmealDish -> setmealDish.setSetmealId(setmealId));
      // 使用 MyBatis Plus 的批量保存方法
      setmealDishService.saveBatch(setmealDishes);
    }
  }

  /**
   * 批量删除套餐.
   *
   * @param ids 套餐ID列表
   */
  @Override
  @Transactional
  public void deleteBatch(List<Long> ids) {
    // 1. 检查是否有起售中的套餐，使用 MyBatis Plus 的 exists 优化
    // 生成 SQL: SELECT 1 FROM setmeal WHERE id IN (...) AND status = 1 LIMIT 1
    boolean exists = lambdaQuery()
        .in(Setmeal::getId, ids)
        .eq(Setmeal::getStatus, StatusConstant.ENABLE)
        .exists();

    if (exists) {
      // 起售中的套餐不能删除
      throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
    }

    // 2. 删除套餐和菜品的关联关系，使用 MyBatis Plus 的链式调用
    // 语义：DELETE FROM setmeal_dish WHERE setmeal_id IN (1, 2, 3)
    setmealDishService.lambdaUpdate()
        .in(SetmealDish::getSetmealId, ids)
        .remove();

    // 3. 删除套餐表中的数据，使用 MyBatis Plus 的批量删除方法
    this.removeByIds(ids);
  }

  /**
   * 套餐起售、停售.
   *
   * @param status 状态（1-起售，0-停售）
   * @param id     套餐ID
   */
  @Override
  public void startOrStop(Integer status, Long id) {
    // 起售套餐时，判断套餐内是否有停售菜品，有停售菜品提示"套餐内包含未启售菜品，无法启售"
    if (StatusConstant.ENABLE.equals(status) && dishSetmealRelationService.hasDisabledDishInSetmeal(
        id)) {
      throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
    }

    // 5. 使用 MyBatis Plus 的链式更新方法更新套餐状态
    lambdaUpdate()
        .eq(Setmeal::getId, id)
        .set(Setmeal::getStatus, status)
        .set(Setmeal::getUpdateTime, LocalDateTime.now())
        .set(Setmeal::getUpdateUser, BaseContext.getCurrentId())
        .update();
  }
}
