package dev.kaiwen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dev.kaiwen.entity.Dish;
import org.apache.ibatis.annotations.Mapper;

/**
 * 菜品 Mapper 接口.
 */
@Mapper
public interface DishMapper extends BaseMapper<Dish> {

}
