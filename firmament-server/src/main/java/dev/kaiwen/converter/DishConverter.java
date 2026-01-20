package dev.kaiwen.converter;

import dev.kaiwen.dto.DishDto;
import dev.kaiwen.entity.Dish;
import dev.kaiwen.vo.DishVo;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Dish 转换器. 使用 MapStruct 自动生成实现类.
 */
@Mapper
public interface DishConverter {

  DishConverter INSTANCE = Mappers.getMapper(DishConverter.class);

  /**
   * DTO -> Entity (用于新增和修改菜品).
   *
   * @param dishDto 菜品DTO
   * @return 菜品实体
   */
  Dish d2e(DishDto dishDto);

  /**
   * Entity -> VO (用于查询返回).
   *
   * @param dish 菜品实体
   * @return 菜品VO
   */
  DishVo e2v(Dish dish);

  /**
   * Entity List -> VO List (用于批量查询返回).
   * 注意：当前代码中使用 Stream 流进行转换，此方法保留作为备用选项.
   *
   * @param list 菜品实体列表
   * @return 菜品VO列表
   */
  @SuppressWarnings("unused")
  List<DishVo> entityListToVoList(List<Dish> list);
}
