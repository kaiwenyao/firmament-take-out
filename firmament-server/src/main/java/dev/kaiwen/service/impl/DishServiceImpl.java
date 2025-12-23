package dev.kaiwen.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import dev.kaiwen.controller.admin.DishController;
import dev.kaiwen.converter.DishConverter;
import dev.kaiwen.dto.DishDTO;
import dev.kaiwen.dto.DishPageQueryDTO;
import dev.kaiwen.entity.Category;
import dev.kaiwen.entity.Dish;
import dev.kaiwen.entity.DishFlavor;
import dev.kaiwen.entity.Employee;
import dev.kaiwen.mapper.DishMapper;
import dev.kaiwen.result.PageResult;
import dev.kaiwen.service.ICategoryService;
import dev.kaiwen.service.IDishFlavorService;
import dev.kaiwen.service.IDishService;
import dev.kaiwen.vo.DishVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements IDishService {


    private final DishConverter dishConverter;
    private final IDishFlavorService dishFlavorService;
    private final ICategoryService categoryService;

    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        // 向菜品表插入数据
        Dish dish = dishConverter.d2e(dishDTO);
        this.save(dish);
        Long dishId = dish.getId();
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(f -> f.setDishId(dishId));
            dishFlavorService.saveBatch(flavors);
        }

    }

    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        Page<Dish> pageInfo = new Page<>(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());

        lambdaQuery() // 开启链式查询，底层创建了 LambdaQueryChainWrapper
                // ▼ name: 模糊查询 (like)
                // 第一个参数是 boolean：只有当 name 有值得时候，才会拼接这条 SQL
                .like(StringUtils.hasText(dishPageQueryDTO.getName()),
                        Dish::getName, dishPageQueryDTO.getName())

                // ▼ categoryId: 精确查询 (eq)
                // 只有当 categoryId 不为 null 时才拼接
                .eq(dishPageQueryDTO.getCategoryId() != null,
                        Dish::getCategoryId, dishPageQueryDTO.getCategoryId())

                // ▼ status: 精确查询 (eq)
                // 只有当 status 不为 null 时才拼接
                .eq(dishPageQueryDTO.getStatus() != null,
                        Dish::getStatus, dishPageQueryDTO.getStatus())
                .orderByDesc(Dish::getCreateTime)
                .page(pageInfo);

        // ================== 分割线：下面是 Entity 转 VO 的过程 ==================

        // 2. 取出原始数据 List<Dish>
        List<Dish> records = pageInfo.getRecords();
        // 2. 提取所有的分类 ID (去重)
        Set<Long> categoryIds = records.stream()
                .map(Dish::getCategoryId)
                .filter(Objects::nonNull) // 过滤掉没有分类ID的数据，防止报错
                .collect(Collectors.toSet());
        // 3. 批量查询分类 (1次 SQL: SELECT ... WHERE id IN (...))
        Map<Long, String> categoryMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            List<Category> categories = categoryService.listByIds(categoryIds);

            // 4. 将 List 转为 Map<ID, Name>，方便后面 O(1) 级别的快速查找
            categoryMap = categories.stream()
                    .collect(Collectors.toMap(Category::getId, Category::getName));
        }

        // 5. 组装 VO
        // 这里需要一个 effectively final 的 map 给 lambda 用
        Map<Long, String> finalCategoryMap = categoryMap;
        List<DishVO> voList = records.stream().map(dish -> {
            // 5.1 属性拷贝
            DishVO dishVO = dishConverter.e2v(dish);

            // 5.2 从 Map 中直接取名字，不再查库
            // getOrDefault 防止 map 里找不到报错，给个默认值或null
            String categoryName = finalCategoryMap.get(dish.getCategoryId());
            dishVO.setCategoryName(categoryName);

            return dishVO;
        }).collect(Collectors.toList());

        return new PageResult(pageInfo.getTotal(), voList);
    }
}
