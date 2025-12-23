package dev.kaiwen.converter;

import dev.kaiwen.dto.DishDTO;
import dev.kaiwen.dto.EmployeeDTO;
import dev.kaiwen.entity.Dish;
import dev.kaiwen.entity.Employee;
import dev.kaiwen.vo.DishVO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DishConverter {
    Dish d2e(DishDTO dishDTO);
    DishVO e2v(Dish dish);
    // 集合转换 (可选，也可以用 Stream流 自己写)
    List<DishVO> eL2v(List<Dish> list);
}
