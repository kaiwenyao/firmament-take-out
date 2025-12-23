package dev.kaiwen.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import dev.kaiwen.controller.admin.DishController;
import dev.kaiwen.converter.DishConverter;
import dev.kaiwen.dto.DishDTO;
import dev.kaiwen.entity.Dish;
import dev.kaiwen.entity.DishFlavor;
import dev.kaiwen.mapper.DishMapper;
import dev.kaiwen.service.IDishFlavorService;
import dev.kaiwen.service.IDishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements IDishService {


    private final DishConverter dishConverter;
    private final IDishFlavorService dishFlavorService;
    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        // 向菜品表插入数据
        Dish dish = dishConverter.d2e(dishDTO);
        this.save(dish);
        Long dishId = dish.getId();
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && flavors.size() > 0) {
            flavors.forEach(f -> f.setDishId(dishId));
            dishFlavorService.saveBatch(flavors);
        }

    }
}
