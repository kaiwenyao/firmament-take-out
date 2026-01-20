package dev.kaiwen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import dev.kaiwen.dto.DishDto;
import dev.kaiwen.dto.DishPageQueryDto;
import dev.kaiwen.entity.Dish;
import dev.kaiwen.result.PageResult;
import dev.kaiwen.vo.DishVo;

import java.util.List;


public interface DishService extends IService<Dish> {
    public void saveWithFlavor(DishDto dishDTO);

    PageResult pageQuery(DishPageQueryDto dishPageQueryDTO);

    void deleteDish(List<Long> ids);

    DishVo getDishById(Long id);

    void updateDish(DishDto dishDTO);

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    List<DishVo> listWithFlavor(Dish dish);

    /**
     * 菜品起售、停售
     * @param status
     * @param id
     */
    void startOrStop(Integer status, Long id);
}

