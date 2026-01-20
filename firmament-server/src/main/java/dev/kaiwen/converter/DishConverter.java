package dev.kaiwen.converter;

import dev.kaiwen.dto.DishDto;
import dev.kaiwen.entity.Dish;
import dev.kaiwen.vo.DishVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface DishConverter {
    
    DishConverter INSTANCE = Mappers.getMapper(DishConverter.class);
    
    Dish d2e(DishDto dishDTO);
    DishVO e2v(Dish dish);
    // 集合转换 (可选，也可以用 Stream流 自己写)
    List<DishVO> eL2v(List<Dish> list);
}
