package dev.kaiwen.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import dev.kaiwen.constant.MessageConstant;
import dev.kaiwen.constant.StatusConstant;
import dev.kaiwen.context.BaseContext;
import dev.kaiwen.converter.CategoryConverter;
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
import dev.kaiwen.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
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
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private CategoryConverter categoryConverter;


    /**
     * 新增分类
     * @param categoryDTO
     */
    public void save(CategoryDTO categoryDTO) {
        // 使用 MapStruct 进行对象转换
        Category category = categoryConverter.d2e(categoryDTO);

        //分类状态默认为禁用状态0
        category.setStatus(StatusConstant.DISABLE);

        // 使用 ServiceImpl 提供的 save 方法
        // 注意：createTime、updateTime、createUser、updateUser 会通过 AutoFillMetaObjectHandler 自动填充
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
        // 注意：page() 方法会直接修改传入的 page 对象（引用传递），填充 total 和 records
        lambdaQuery()
                .like(StringUtils.hasText(categoryPageQueryDTO.getName()), 
                        Category::getName, categoryPageQueryDTO.getName())
                .eq(categoryPageQueryDTO.getType() != null, 
                        Category::getType, categoryPageQueryDTO.getType())
                .orderByAsc(Category::getSort)
                .orderByDesc(Category::getCreateTime)
                .page(page);
        
        // 直接从 page 对象中获取填充好的数据
        return new PageResult(page.getTotal(), page.getRecords());
    }

    /**
     * 根据id删除分类
     * @param id
     */
    public void deleteById(Long id) {
        //查询当前分类是否关联了菜品，如果关联了就抛出业务异常
        // 使用链式调用检查是否存在

        boolean dishExists = Db.lambdaQuery(Dish.class)
                .eq(Dish::getCategoryId, id)
                .exists();
        if(dishExists){
            //当前分类下有菜品，不能删除
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_DISH);
        }

        //查询当前分类是否关联了套餐，如果关联了就抛出业务异常
        boolean setmealExists = Db.lambdaQuery(Setmeal.class)
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
        // 使用 MapStruct 进行对象转换
        Category category = categoryConverter.d2e(categoryDTO);

        // 使用 ServiceImpl 提供的 updateById 方法，只更新非空字段
        // 注意：updateTime、updateUser 会通过 AutoFillMetaObjectHandler 自动填充
        this.updateById(category);
    }

    /**
     * 启用、禁用分类
     * @param status
     * @param id
     */
    public void enableOrDisable(Integer status, Long id) {
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
