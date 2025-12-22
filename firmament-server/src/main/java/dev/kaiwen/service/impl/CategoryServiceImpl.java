package dev.kaiwen.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import dev.kaiwen.constant.MessageConstant;
import dev.kaiwen.constant.StatusConstant;
import dev.kaiwen.context.BaseContext;
import dev.kaiwen.dto.CategoryDTO;
import dev.kaiwen.dto.CategoryPageQueryDTO;
import dev.kaiwen.entity.Category;
import dev.kaiwen.entity.Dish;
import dev.kaiwen.entity.Setmeal;
import dev.kaiwen.exception.DeletionNotAllowedException;
import dev.kaiwen.mapper.CategoryMapper;
import dev.kaiwen.mapper.DishMapper;
import dev.kaiwen.mapper.SetmealMapper;
import dev.kaiwen.result.PageResult;
import dev.kaiwen.service.ICategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 分类业务层
 */
@Service
@Slf4j
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements ICategoryService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;


    /**
     * 新增分类
     * @param categoryDTO
     */
    public void save(CategoryDTO categoryDTO) {
        Category category = new Category();
        //属性拷贝
        BeanUtils.copyProperties(categoryDTO, category);

        //分类状态默认为禁用状态0
        category.setStatus(StatusConstant.DISABLE);

        //设置创建时间、修改时间、创建人、修改人
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        category.setCreateUser(BaseContext.getCurrentId());
        category.setUpdateUser(BaseContext.getCurrentId());

        // 使用 ServiceImpl 提供的 save 方法
        this.save(category);
    }

    /**
     * 分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    public PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO) {
        // 使用 MyBatis Plus 分页插件
        Page<Category> page = new Page<>(categoryPageQueryDTO.getPage(), categoryPageQueryDTO.getPageSize());
        
        // 使用链式调用构建查询条件并执行分页查询
        Page<Category> pageResult = lambdaQuery()
                .like(StringUtils.hasText(categoryPageQueryDTO.getName()), 
                        Category::getName, categoryPageQueryDTO.getName())
                .eq(categoryPageQueryDTO.getType() != null, 
                        Category::getType, categoryPageQueryDTO.getType())
                .orderByAsc(Category::getSort)
                .orderByDesc(Category::getCreateTime)
                .page(page);
        
        return new PageResult(pageResult.getTotal(), pageResult.getRecords());
    }

    /**
     * 根据id删除分类
     * @param id
     */
    public void deleteById(Long id) {
        //查询当前分类是否关联了菜品，如果关联了就抛出业务异常
        // 使用链式调用检查是否存在
        boolean dishExists = new LambdaQueryChainWrapper<>(dishMapper)
                .eq(Dish::getCategoryId, id)
                .exists();
        if(dishExists){
            //当前分类下有菜品，不能删除
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_DISH);
        }

        //查询当前分类是否关联了套餐，如果关联了就抛出业务异常
        boolean setmealExists = new LambdaQueryChainWrapper<>(setmealMapper)
                .eq(Setmeal::getCategoryId, id)
                .exists();
        if(setmealExists){
            //当前分类下有套餐，不能删除
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL);
        }

        //删除分类数据，使用 ServiceImpl 提供的 removeById 方法
        this.removeById(id);
    }

    /**
     * 修改分类
     * @param categoryDTO
     */
    public void update(CategoryDTO categoryDTO) {
        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO,category);

        //设置修改时间、修改人
        category.setUpdateTime(LocalDateTime.now());
        category.setUpdateUser(BaseContext.getCurrentId());

        // 使用 ServiceImpl 提供的 updateById 方法，只更新非空字段
        this.updateById(category);
    }

    /**
     * 启用、禁用分类
     * @param status
     * @param id
     */
    public void startOrStop(Integer status, Long id) {
        // 使用链式调用进行更新
        lambdaUpdate()
                .eq(Category::getId, id)
                .set(Category::getStatus, status)
                .set(Category::getUpdateTime, LocalDateTime.now())
                .set(Category::getUpdateUser, BaseContext.getCurrentId())
                .update();
    }

    /**
     * 根据类型查询分类
     * @param type
     * @return
     */
    public List<Category> list(Integer type) {
        // 使用链式调用构建查询条件
        return lambdaQuery()
                .eq(Category::getStatus, StatusConstant.ENABLE)
                .eq(type != null, Category::getType, type)
                .orderByAsc(Category::getSort)
                .orderByDesc(Category::getCreateTime)
                .list();
    }
}
