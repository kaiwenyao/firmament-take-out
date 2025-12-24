package dev.kaiwen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import dev.kaiwen.dto.DishDTO;
import dev.kaiwen.dto.DishPageQueryDTO;
import dev.kaiwen.entity.Dish;
import dev.kaiwen.result.PageResult;
import dev.kaiwen.result.Result;
import dev.kaiwen.vo.DishVO;

import java.util.List;


public interface IDishService extends IService<Dish> {
    public void saveWithFlavor(DishDTO dishDTO);

    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    void deleteDish(List<Long> ids);

    DishVO getDishById(Long id);

    void updateDish(DishDTO dishDTO);

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    List<DishVO> listWithFlavor(Dish dish);

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    List<Dish> list(Long categoryId);

    /**
     * 菜品起售、停售
     * @param status
     * @param id
     */
    void startOrStop(Integer status, Long id);
}
