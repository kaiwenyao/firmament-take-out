package dev.kaiwen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import dev.kaiwen.dto.SetmealDto;
import dev.kaiwen.dto.SetmealPageQueryDto;
import dev.kaiwen.entity.Setmeal;
import dev.kaiwen.result.PageResult;
import dev.kaiwen.vo.DishItemVo;
import dev.kaiwen.vo.SetmealVo;
import java.util.List;

/**
 * 套餐服务接口.
 */
public interface SetmealService extends IService<Setmeal> {

  /**
   * 新增套餐，同时需要保存套餐和菜品的关联关系.
   *
   * @param setmealDto 套餐DTO
   */
  void saveWithDish(SetmealDto setmealDto);

  /**
   * 条件查询.
   *
   * @param setmeal 套餐实体
   * @return 套餐列表
   */
  List<Setmeal> list(Setmeal setmeal);

  /**
   * 根据id查询菜品选项.
   *
   * @param id 套餐ID
   * @return 菜品项VO列表
   */
  List<DishItemVo> getDishItemById(Long id);

  /**
   * 分页查询.
   *
   * @param setmealPageQueryDto 套餐分页查询DTO
   * @return 分页结果
   */
  PageResult pageQuery(SetmealPageQueryDto setmealPageQueryDto);

  /**
   * 根据id查询套餐和关联的菜品数据.
   *
   * @param id 套餐ID
   * @return 套餐VO
   */
  SetmealVo getByIdWithDish(Long id);

  /**
   * 修改套餐.
   *
   * @param setmealDto 套餐DTO
   */
  void update(SetmealDto setmealDto);

  /**
   * 批量删除套餐.
   *
   * @param ids 套餐ID列表
   */
  void deleteBatch(List<Long> ids);

  /**
   * 套餐起售、停售.
   *
   * @param status 状态
   * @param id     套餐ID
   */
  void startOrStop(Integer status, Long id);
}

