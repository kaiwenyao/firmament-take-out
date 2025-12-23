package dev.kaiwen.converter;

import dev.kaiwen.dto.DishDTO;
import dev.kaiwen.dto.EmployeeDTO;
import dev.kaiwen.entity.Dish;
import dev.kaiwen.entity.Employee;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DishConverter {
    Dish d2e(DishDTO dishDTO);

}
