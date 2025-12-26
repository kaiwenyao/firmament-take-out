package dev.kaiwen.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import dev.kaiwen.constant.MessageConstant;
import dev.kaiwen.constant.StatusConstant;
import dev.kaiwen.converter.DishConverter;
import dev.kaiwen.dto.DishDTO;
import dev.kaiwen.dto.DishPageQueryDTO;
import dev.kaiwen.entity.*;
import dev.kaiwen.exception.DeletionNotAllowedException;
import dev.kaiwen.exception.DishDisableFailedException;
import dev.kaiwen.context.BaseContext;
import dev.kaiwen.mapper.DishMapper;
import dev.kaiwen.result.PageResult;
import dev.kaiwen.service.CategoryService;
import dev.kaiwen.service.DishFlavorService;
import dev.kaiwen.service.DishService;
import dev.kaiwen.service.DishSetmealRelationService;
import dev.kaiwen.service.SetmealDishService;
import dev.kaiwen.vo.DishVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {


    private final DishConverter dishConverter;
    private final DishFlavorService dishFlavorService;
    private final CategoryService categoryService;
    private final SetmealDishService setmealDishService;
    private final DishSetmealRelationService dishSetmealRelationService;
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

    @Override
    @Transactional
    public void deleteDish(List<Long> ids) {
        boolean exists = lambdaQuery()
                .in(Dish::getId, ids)
                .eq(Dish::getStatus, StatusConstant.ENABLE)
                .exists(); // 生成 SQL: SELECT 1 FROM dish WHERE ... LIMIT 1

        if (exists) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
        }
        // 2. 检查是否被套餐关联 (使用 exists 优化)
        // 语义：去 setmeal_dish 表查看，只要 ids 中有任何一个出现在 dish_id 列中，就返回 true
        boolean isRelated = setmealDishService.lambdaQuery()
                .in(SetmealDish::getDishId, ids)
                .exists();

        if (isRelated) {
            // 存在关联数据，抛出异常
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        // 可以删除
        this.removeByIds(ids);
        // 删除关联的口味数据 (dish_flavor 表)
        // 语义：DELETE FROM dish_flavor WHERE dish_id IN (1, 2, 3)
        dishFlavorService.lambdaUpdate() // 开启链式更新/删除
                .in(DishFlavor::getDishId, ids) // 指定条件：dish_id 在 ids 列表中
                .remove(); // 执行删除操作

    }

    @Override
    public DishVO getDishById(Long id) {
        Dish dish = this.getById(id);
        DishVO dishVO = dishConverter.e2v(dish);
        List<DishFlavor> dishFlavors = dishFlavorService.lambdaQuery()
                .in(DishFlavor::getDishId, id)
                .list();
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }

    @Override
    public void updateDish(DishDTO dishDTO) {
        // 基本信息
        Dish dish = dishConverter.d2e(dishDTO);
        this.updateById(dish);
        // 口味先删除再插入
        dishFlavorService.lambdaUpdate()
                .in(DishFlavor::getDishId, dish.getId())
                .remove();
        List<DishFlavor> flavors = dishDTO.getFlavors();
        // 判空保护：只有前端真的传了口味，我们才执行插入
        if (flavors != null && !flavors.isEmpty()) {
            // ⚠️ 核心步骤：关联外键
            // 前端传来的 flavor 对象里通常没有 dishId，必须手动把当前菜品的 ID 赋给它们
            flavors.forEach(flavor -> {
                flavor.setDishId(dish.getId());
            });
            // 批量插入数据库
            // 对应 SQL: INSERT INTO dish_flavor (dish_id, name, value) VALUES (?,?,?), (?,?,?)...
            dishFlavorService.saveBatch(flavors);
        }

    }

    @Override
    public List<DishVO> listWithFlavor(Dish dish) {

        // 1. 构造查询条件并查询菜品列表
        List<Dish> dishList = this.lambdaQuery()
                .eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId()) // 动态拼接categoryId
                .eq(dish.getStatus() != null, Dish::getStatus, dish.getStatus()) // 动态拼接status(比如只查起售的)
                .list();
        // 如果没查到菜品，直接返回空集合
        if (dishList == null || dishList.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 将 Dish 转为 DishVO
        // 或者用你的
        List<DishVO> dishVOList = dishList.stream().map(dishConverter::e2v).collect(Collectors.toList());


        // ================= 核心优化开始 =================

        // 3. 提取所有菜品的 ID 集合 (如: [100, 101, 102])
        List<Long> dishIds = dishList.stream()
                .map(Dish::getId)
                .collect(Collectors.toList());

        // 4. 一次性查询这些菜品对应的所有口味 (Select * from dish_flavor where dish_id IN (...))
        List<DishFlavor> allFlavors = dishFlavorService.lambdaQuery()
                .in(DishFlavor::getDishId, dishIds)
                .list();

        // 5. 在内存中将口味按 dishId 分组 (Map<Long, List<DishFlavor>>)
        // 结果类似：{100: [微辣, 少冰], 101: [重辣]}
        Map<Long, List<DishFlavor>> flavorMap = allFlavors.stream()
                .collect(Collectors.groupingBy(DishFlavor::getDishId));

        // 6. 遍历 VO 列表，从 Map 中直接取值填充，不再查库
        dishVOList.forEach(vo -> {
            vo.setFlavors(flavorMap.get(vo.getId()));
        });

        // ================= 核心优化结束 =================

        return dishVOList;
    }
    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    @Override
    public List<Dish> list(Long categoryId) {
        // 使用 MyBatis Plus 的链式查询
        return lambdaQuery()
                .eq(categoryId != null, Dish::getCategoryId, categoryId)
                .eq(Dish::getStatus, StatusConstant.ENABLE) // 只查询起售中的菜品
                .orderByDesc(Dish::getCreateTime) // 按创建时间降序
                .list();
    }

    /**
     * 菜品起售、停售
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        // 停售菜品时，判断是否有起售的套餐在使用这个菜品，有起售套餐提示"菜品关联了起售中的套餐，无法停售"
        if (status == StatusConstant.DISABLE) {
            // 使用关系检查服务，避免循环依赖
            if (dishSetmealRelationService.hasEnabledSetmealUsingDish(id)) {
                throw new DishDisableFailedException(MessageConstant.DISH_DISABLE_FAILED);
            }
        }

        // 5. 使用 MyBatis Plus 的链式更新方法更新菜品状态
        lambdaUpdate()
                .eq(Dish::getId, id)
                .set(Dish::getStatus, status)
                .set(Dish::getUpdateTime, LocalDateTime.now())
                .set(Dish::getUpdateUser, BaseContext.getCurrentId())
                .update();
    }
}
