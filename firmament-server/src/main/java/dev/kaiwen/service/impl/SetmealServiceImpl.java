package dev.kaiwen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import dev.kaiwen.context.BaseContext;
import dev.kaiwen.constant.MessageConstant;
import dev.kaiwen.constant.StatusConstant;
import dev.kaiwen.converter.SetmealConverter;
import dev.kaiwen.dto.SetmealDTO;
import dev.kaiwen.dto.SetmealPageQueryDTO;
import dev.kaiwen.entity.Category;
import dev.kaiwen.entity.Dish;
import dev.kaiwen.entity.Setmeal;
import dev.kaiwen.entity.SetmealDish;
import dev.kaiwen.exception.DeletionNotAllowedException;
import dev.kaiwen.exception.SetmealEnableFailedException;
import dev.kaiwen.mapper.DishMapper;
import dev.kaiwen.mapper.SetmealMapper;
import dev.kaiwen.result.PageResult;
import dev.kaiwen.service.ICategoryService;
import dev.kaiwen.service.IDishSetmealRelationService;
import dev.kaiwen.service.ISetmealDishService;
import dev.kaiwen.service.ISetmealService;
import dev.kaiwen.vo.DishItemVO;
import dev.kaiwen.vo.SetmealVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements ISetmealService {
    private final SetmealConverter setmealConverter;
    private final ISetmealDishService setmealDishService;
    private final ICategoryService categoryService;
    private final IDishSetmealRelationService dishSetmealRelationService;
    private final DishMapper dishMapper;

    /**
     * 新增套餐，同时需要保存套餐和菜品的关联关系
     * @param setmealDTO
     */
    @Override
    @Transactional
    public void saveWithDish(SetmealDTO setmealDTO) {
        // 使用 MapStruct 进行对象转换
        Setmeal setmeal = setmealConverter.d2e(setmealDTO);

        // 向套餐表插入数据，使用 MyBatis Plus 的 save 方法
        this.save(setmeal);

        // 获取生成的套餐id
        Long setmealId = setmeal.getId();

        // 保存套餐和菜品的关联关系
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if (setmealDishes != null && !setmealDishes.isEmpty()) {
            setmealDishes.forEach(setmealDish -> {
                setmealDish.setSetmealId(setmealId);
            });
            // 使用 MyBatis Plus 的批量保存方法
            setmealDishService.saveBatch(setmealDishes);
        }
    }

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    @Override
    public List<Setmeal> list(Setmeal setmeal) {
        // 使用 MyBatis Plus 的链式调用构建动态查询条件
        return lambdaQuery()
                .eq(setmeal.getCategoryId() != null, Setmeal::getCategoryId, setmeal.getCategoryId())
                .eq(setmeal.getStatus() != null, Setmeal::getStatus, setmeal.getStatus())
                .like(StringUtils.hasText(setmeal.getName()), Setmeal::getName, setmeal.getName())
                .orderByDesc(Setmeal::getCreateTime)
                .list();
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    @Override
    public List<DishItemVO> getDishItemById(Long id) {
        // 1. 查询套餐关联的菜品关系，获取 dishId 和 copies
        List<SetmealDish> setmealDishes = setmealDishService.lambdaQuery()
                .eq(SetmealDish::getSetmealId, id)
                .list();
        
        if (setmealDishes == null || setmealDishes.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 2. 提取菜品ID列表
        List<Long> dishIds = setmealDishes.stream()
                .map(SetmealDish::getDishId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        if (dishIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 3. 批量查询菜品信息（直接使用 Mapper，避免 Service 层循环依赖）
        List<Dish> dishList = dishMapper.selectList(
                new LambdaQueryWrapper<Dish>().in(Dish::getId, dishIds)
        );
        
        // 4. 构建 dishId -> SetmealDish 的映射，方便快速查找 copies
        Map<Long, SetmealDish> setmealDishMap = setmealDishes.stream()
                .collect(Collectors.toMap(SetmealDish::getDishId, sd -> sd, (existing, replacement) -> existing));
        
        // 5. 组装 DishItemVO
        return dishList.stream()
                .map(dish -> {
                    SetmealDish setmealDish = setmealDishMap.get(dish.getId());
                    return DishItemVO.builder()
                            .name(dish.getName())
                            .copies(setmealDish != null ? setmealDish.getCopies() : 1)
                            .image(dish.getImage())
                            .description(dish.getDescription())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        // 使用 MyBatis Plus 分页插件
        Page<Setmeal> pageInfo = new Page<>(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());

        // 使用链式调用构建查询条件并执行分页查询
        lambdaQuery()
                // name: 模糊查询 (like)
                .like(StringUtils.hasText(setmealPageQueryDTO.getName()),
                        Setmeal::getName, setmealPageQueryDTO.getName())
                // categoryId: 精确查询 (eq)
                .eq(setmealPageQueryDTO.getCategoryId() != null,
                        Setmeal::getCategoryId, setmealPageQueryDTO.getCategoryId())
                // status: 精确查询 (eq)
                .eq(setmealPageQueryDTO.getStatus() != null,
                        Setmeal::getStatus, setmealPageQueryDTO.getStatus())
                .orderByDesc(Setmeal::getCreateTime)
                .page(pageInfo);

        // ================== 分割线：下面是 Entity 转 VO 的过程 ==================

        // 取出原始数据 List<Setmeal>
        List<Setmeal> records = pageInfo.getRecords();
        
        // 提取所有的分类 ID (去重)
        Set<Long> categoryIds = records.stream()
                .map(Setmeal::getCategoryId)
                .filter(Objects::nonNull) // 过滤掉没有分类ID的数据，防止报错
                .collect(Collectors.toSet());
        
        // 批量查询分类 (1次 SQL: SELECT ... WHERE id IN (...))
        Map<Long, String> categoryMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            List<Category> categories = categoryService.listByIds(categoryIds);

            // 将 List 转为 Map<ID, Name>，方便后面 O(1) 级别的快速查找
            categoryMap = categories.stream()
                    .collect(Collectors.toMap(Category::getId, Category::getName));
        }

        // 组装 VO
        // 这里需要一个 effectively final 的 map 给 lambda 用
        Map<Long, String> finalCategoryMap = categoryMap;
        List<SetmealVO> voList = records.stream().map(setmeal -> {
            // 属性拷贝
            SetmealVO setmealVO = setmealConverter.e2v(setmeal);

            // 从 Map 中直接取名字，不再查库
            String categoryName = finalCategoryMap.get(setmeal.getCategoryId());
            setmealVO.setCategoryName(categoryName);

            return setmealVO;
        }).collect(Collectors.toList());

        return new PageResult(pageInfo.getTotal(), voList);
    }

    /**
     * 根据id查询套餐和套餐菜品关系
     *
     * @param id
     * @return
     */
    @Override
    public SetmealVO getByIdWithDish(Long id) {
        // 使用 MyBatis Plus 查询套餐基本信息
        Setmeal setmeal = this.getById(id);
        
        // 使用 MapStruct 进行对象转换
        SetmealVO setmealVO = setmealConverter.e2v(setmeal);
        
        // 使用 MyBatis Plus 查询套餐和菜品的关联关系
        List<SetmealDish> setmealDishes = setmealDishService.lambdaQuery()
                .eq(SetmealDish::getSetmealId, id)
                .list();
        
        setmealVO.setSetmealDishes(setmealDishes);
        
        return setmealVO;
    }

    /**
     * 修改套餐
     *
     * @param setmealDTO
     */
    @Override
    @Transactional
    public void update(SetmealDTO setmealDTO) {
        // 使用 MapStruct 进行对象转换
        Setmeal setmeal = setmealConverter.d2e(setmealDTO);
        
        // 1. 修改套餐表，使用 MyBatis Plus 的 updateById 方法
        this.updateById(setmeal);
        
        // 套餐id
        Long setmealId = setmealDTO.getId();
        
        // 2. 删除套餐和菜品的关联关系，使用 MyBatis Plus 的链式调用
        setmealDishService.lambdaUpdate()
                .eq(SetmealDish::getSetmealId, setmealId)
                .remove();
        
        // 3. 重新插入套餐和菜品的关联关系
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if (setmealDishes != null && !setmealDishes.isEmpty()) {
            setmealDishes.forEach(setmealDish -> {
                setmealDish.setSetmealId(setmealId);
            });
            // 使用 MyBatis Plus 的批量保存方法
            setmealDishService.saveBatch(setmealDishes);
        }
    }
    /**
     * 批量删除套餐
     * @param ids
     */
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        // 1. 检查是否有起售中的套餐，使用 MyBatis Plus 的 exists 优化
        // 生成 SQL: SELECT 1 FROM setmeal WHERE id IN (...) AND status = 1 LIMIT 1
        boolean exists = lambdaQuery()
                .in(Setmeal::getId, ids)
                .eq(Setmeal::getStatus, StatusConstant.ENABLE)
                .exists();

        if (exists) {
            // 起售中的套餐不能删除
            throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
        }

        // 2. 删除套餐和菜品的关联关系，使用 MyBatis Plus 的链式调用
        // 语义：DELETE FROM setmeal_dish WHERE setmeal_id IN (1, 2, 3)
        setmealDishService.lambdaUpdate()
                .in(SetmealDish::getSetmealId, ids)
                .remove();

        // 3. 删除套餐表中的数据，使用 MyBatis Plus 的批量删除方法
        this.removeByIds(ids);
    }

    /**
     * 套餐起售、停售
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        // 起售套餐时，判断套餐内是否有停售菜品，有停售菜品提示"套餐内包含未启售菜品，无法启售"
        if (status == StatusConstant.ENABLE) {
            // 使用关系检查服务，避免循环依赖
            if (dishSetmealRelationService.hasDisabledDishInSetmeal(id)) {
                throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
            }
        }

        // 5. 使用 MyBatis Plus 的链式更新方法更新套餐状态
        lambdaUpdate()
                .eq(Setmeal::getId, id)
                .set(Setmeal::getStatus, status)
                .set(Setmeal::getUpdateTime, LocalDateTime.now())
                .set(Setmeal::getUpdateUser, BaseContext.getCurrentId())
                .update();
    }
}
