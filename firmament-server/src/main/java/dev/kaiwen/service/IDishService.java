package dev.kaiwen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import dev.kaiwen.dto.DishDTO;
import dev.kaiwen.entity.Dish;


public interface IDishService extends IService<Dish> {
    public void saveWithFlavor(DishDTO dishDTO);
}
