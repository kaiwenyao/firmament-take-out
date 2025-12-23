package dev.kaiwen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import dev.kaiwen.dto.DishDTO;
import dev.kaiwen.dto.DishPageQueryDTO;
import dev.kaiwen.entity.Dish;
import dev.kaiwen.result.PageResult;

import java.util.List;


public interface IDishService extends IService<Dish> {
    public void saveWithFlavor(DishDTO dishDTO);

    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    void deleteDish(List<Long> ids);
}
