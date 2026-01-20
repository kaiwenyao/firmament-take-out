package dev.kaiwen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import dev.kaiwen.dto.DishDto;
import dev.kaiwen.dto.DishPageQueryDto;
import dev.kaiwen.entity.Dish;
import dev.kaiwen.result.PageResult;
import dev.kaiwen.vo.DishVo;
import java.util.List;

/**
 * 菜品服务接口.
 */
public interface DishService extends IService<Dish> {

  /**
   * 新增菜品，同时保存菜品和口味的关联关系.
   *
   * @param dishDto 菜品DTO
   */
  void saveWithFlavor(DishDto dishDto);

  /**
   * 分页查询菜品.
   *
   * @param dishPageQueryDto 菜品分页查询DTO
   * @return 分页结果
   */
  PageResult pageQuery(DishPageQueryDto dishPageQueryDto);

  /**
   * 批量删除菜品.
   *
   * @param ids 菜品ID列表
   */
  void deleteDish(List<Long> ids);

  /**
   * 根据id查询菜品详情.
   *
   * @param id 菜品ID
   * @return 菜品VO
   */
  DishVo getDishById(Long id);

  /**
   * 修改菜品，同时更新菜品和口味的关联关系.
   *
   * @param dishDto 菜品DTO
   */
  void updateDish(DishDto dishDto);

  /**
   * 条件查询菜品和口味.
   *
   * @param dish 菜品实体
   * @return 菜品VO列表
   */
  List<DishVo> listWithFlavor(Dish dish);

  /**
   * 菜品起售、停售.
   *
   * @param status 状态
   * @param id     菜品ID
   */
  void startOrStop(Integer status, Long id);
}

