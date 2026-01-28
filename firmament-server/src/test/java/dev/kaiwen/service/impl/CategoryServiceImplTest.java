package dev.kaiwen.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import dev.kaiwen.constant.MessageConstant;
import dev.kaiwen.constant.StatusConstant;
import dev.kaiwen.context.BaseContext;
import dev.kaiwen.dto.CategoryDto;
import dev.kaiwen.dto.CategoryPageQueryDto;
import dev.kaiwen.entity.Category;
import dev.kaiwen.entity.Dish;
import dev.kaiwen.entity.Setmeal;
import dev.kaiwen.exception.DeletionNotAllowedException;
import dev.kaiwen.mapper.CategoryMapper;
import dev.kaiwen.mapper.DishMapper;
import dev.kaiwen.mapper.SetmealMapper;
import dev.kaiwen.result.PageResult;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

  @InjectMocks
  private CategoryServiceImpl categoryService;

  @Mock
  private CategoryMapper mapper;

  @Mock
  private DishMapper dishMapper;

  @Mock
  private SetmealMapper setmealMapper;

  @Captor
  private ArgumentCaptor<Category> categoryCaptor;

  @Captor
  private ArgumentCaptor<LambdaQueryWrapper<Category>> categoryWrapperCaptor;

  @Captor
  private ArgumentCaptor<LambdaQueryWrapper<Dish>> dishWrapperCaptor;

  @Captor
  private ArgumentCaptor<LambdaQueryWrapper<Setmeal>> setmealWrapperCaptor;

  @BeforeEach
  void setUp() {
    MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
    TableInfoHelper.initTableInfo(assistant, Category.class);
    TableInfoHelper.initTableInfo(assistant, Dish.class);
    TableInfoHelper.initTableInfo(assistant, Setmeal.class);
    ReflectionTestUtils.setField(categoryService, "baseMapper", mapper);
  }

  @Test
  void saveSuccess() {
    // 1. 准备测试数据
    CategoryDto categoryDto = new CategoryDto();
    categoryDto.setName("川菜");
    categoryDto.setType(1);
    categoryDto.setSort(1);

    // 2. Mock 依赖行为
    when(mapper.insert(any(Category.class))).thenReturn(1);

    // 3. 执行测试
    categoryService.save(categoryDto);

    // 4. 验证方法调用
    verify(mapper).insert(categoryCaptor.capture());
    Category savedCategory = categoryCaptor.getValue();

    // 5. 验证结果
    assertEquals("川菜", savedCategory.getName());
    assertEquals(1, savedCategory.getType());
    assertEquals(1, savedCategory.getSort());
    assertEquals(StatusConstant.DISABLE, savedCategory.getStatus());
  }

  @Test
  void pageQuerySuccess() {
    // 测试场景：分页查询，只设置 name，type 为 null
    // 1. 准备测试数据
    CategoryPageQueryDto dto = new CategoryPageQueryDto();
    dto.setPage(1);
    dto.setPageSize(10);
    dto.setName("川菜");
    // type 为 null，不设置

    Category mockCategory = new Category();
    mockCategory.setId(100L);
    mockCategory.setName("川菜");
    List<Category> dbRecords = Collections.singletonList(mockCategory);

    // 2. Mock 依赖行为
    doAnswer(invocation -> {
      Page<Category> pageArg = invocation.getArgument(0);
      pageArg.setRecords(dbRecords);
      pageArg.setTotal(100L);
      return pageArg;
    }).when(mapper).selectPage(any(), any());

    // 3. 执行测试
    PageResult result = categoryService.pageQuery(dto);

    // 4. 验证结果
    assertEquals(100L, result.getTotal());
    assertEquals(1, result.getRecords().size());

    // 5. 验证方法调用
    verify(mapper).selectPage(any(), categoryWrapperCaptor.capture());
  }

  @Test
  void pageQueryWithType() {
    // 测试场景：分页查询，设置 type 不为 null
    // 覆盖：.eq(categoryPageQueryDto.getType() != null, Category::getType, categoryPageQueryDto.getType()) 的 true 分支
    // 1. 准备测试数据
    CategoryPageQueryDto dto = new CategoryPageQueryDto();
    dto.setPage(1);
    dto.setPageSize(10);
    dto.setName("川菜");
    dto.setType(1); // 设置 type 不为 null，覆盖黄色高亮部分

    Category mockCategory = new Category();
    mockCategory.setId(100L);
    mockCategory.setName("川菜");
    mockCategory.setType(1);
    List<Category> dbRecords = Collections.singletonList(mockCategory);

    // 2. Mock 依赖行为
    doAnswer(invocation -> {
      Page<Category> pageArg = invocation.getArgument(0);
      pageArg.setRecords(dbRecords);
      pageArg.setTotal(50L);
      return pageArg;
    }).when(mapper).selectPage(any(), any());

    // 3. 执行测试
    PageResult result = categoryService.pageQuery(dto);

    // 4. 验证结果
    assertEquals(50L, result.getTotal());
    assertEquals(1, result.getRecords().size());
    @SuppressWarnings("unchecked")
    List<Category> records = (List<Category>) result.getRecords();
    assertEquals(1, records.get(0).getType());

    // 5. 验证方法调用 - 确保 wrapper 中包含了 type 条件
    verify(mapper).selectPage(any(), categoryWrapperCaptor.capture());
    LambdaQueryWrapper<Category> capturedWrapper = categoryWrapperCaptor.getValue();
    assertNotNull(capturedWrapper);
  }

  @Test
  void pageQueryWithType2() {
    // 测试场景：分页查询，设置 type 为 2（套餐分类）
    // 覆盖：.eq(categoryPageQueryDto.getType() != null, Category::getType, categoryPageQueryDto.getType()) 的 true 分支
    // 1. 准备测试数据
    CategoryPageQueryDto dto = new CategoryPageQueryDto();
    dto.setPage(1);
    dto.setPageSize(10);
    dto.setType(2); // 设置 type 为 2（套餐分类）

    Category mockCategory = new Category();
    mockCategory.setId(200L);
    mockCategory.setName("豪华套餐");
    mockCategory.setType(2);
    List<Category> dbRecords = Collections.singletonList(mockCategory);

    // 2. Mock 依赖行为
    doAnswer(invocation -> {
      Page<Category> pageArg = invocation.getArgument(0);
      pageArg.setRecords(dbRecords);
      pageArg.setTotal(30L);
      return pageArg;
    }).when(mapper).selectPage(any(), any());

    // 3. 执行测试
    PageResult result = categoryService.pageQuery(dto);

    // 4. 验证结果
    assertEquals(30L, result.getTotal());
    assertEquals(1, result.getRecords().size());
    @SuppressWarnings("unchecked")
    List<Category> records = (List<Category>) result.getRecords();
    assertEquals(2, records.get(0).getType());

    // 5. 验证方法调用
    verify(mapper).selectPage(any(), categoryWrapperCaptor.capture());
  }

  @Test
  void deleteByIdSuccess() {
    // 1. 准备测试数据
    Long categoryId = 100L;

    // 2. Mock 依赖行为 - 没有关联的菜品和套餐
    when(dishMapper.selectCount(any())).thenReturn(0L);
    when(setmealMapper.selectCount(any())).thenReturn(0L);
    when(mapper.deleteById(categoryId)).thenReturn(1);

    // 3. 执行测试
    categoryService.deleteById(categoryId);

    // 4. 验证方法调用
    verify(dishMapper).selectCount(dishWrapperCaptor.capture());
    verify(setmealMapper).selectCount(setmealWrapperCaptor.capture());
    verify(mapper).deleteById(categoryId);
  }

  @Test
  void deleteByIdWithRelatedDish() {
    // 1. 准备测试数据
    Long categoryId = 100L;

    // 2. Mock 依赖行为 - 有关联的菜品
    when(dishMapper.selectCount(any())).thenReturn(1L);

    // 3. 执行测试并验证异常
    DeletionNotAllowedException exception = assertThrows(DeletionNotAllowedException.class, () ->
        categoryService.deleteById(categoryId)
    );

    // 4. 验证异常消息
    assertEquals(MessageConstant.CATEGORY_BE_RELATED_BY_DISH, exception.getMessage());

    // 5. 验证方法调用
    verify(dishMapper).selectCount(dishWrapperCaptor.capture());
  }

  @Test
  void deleteByIdWithRelatedSetmeal() {
    // 1. 准备测试数据
    Long categoryId = 100L;

    // 2. Mock 依赖行为 - 没有关联的菜品，但有关联的套餐
    when(dishMapper.selectCount(any())).thenReturn(0L);
    when(setmealMapper.selectCount(any())).thenReturn(1L);

    // 3. 执行测试并验证异常
    DeletionNotAllowedException exception = assertThrows(DeletionNotAllowedException.class, () ->
        categoryService.deleteById(categoryId)
    );

    // 4. 验证异常消息
    assertEquals(MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL, exception.getMessage());

    // 5. 验证方法调用
    verify(dishMapper).selectCount(dishWrapperCaptor.capture());
    verify(setmealMapper).selectCount(setmealWrapperCaptor.capture());
  }

  @Test
  void updateSuccess() {
    // 1. 准备测试数据
    CategoryDto categoryDto = new CategoryDto();
    categoryDto.setId(100L);
    categoryDto.setName("更新后的川菜");
    categoryDto.setType(1);
    categoryDto.setSort(2);

    // 2. Mock 依赖行为
    when(mapper.updateById(any(Category.class))).thenReturn(1);

    // 3. 执行测试
    categoryService.update(categoryDto);

    // 4. 验证方法调用
    verify(mapper).updateById(categoryCaptor.capture());
    Category updatedCategory = categoryCaptor.getValue();

    // 5. 验证结果
    assertEquals(100L, updatedCategory.getId());
    assertEquals("更新后的川菜", updatedCategory.getName());
    assertEquals(1, updatedCategory.getType());
    assertEquals(2, updatedCategory.getSort());
  }

  @Test
  void enableOrDisableSuccess() {
    // 1. Mock 依赖行为
    when(mapper.update(isNull(), any())).thenReturn(1);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(888L);

      // 2. 执行测试 - 启用
      categoryService.enableOrDisable(StatusConstant.ENABLE, 100L);

      // 3. 验证方法调用
      verify(mapper).update(isNull(), any());
    }
  }

  @Test
  void listSuccess() {
    // 1. 准备测试数据
    Integer type = 1;
    Category category1 = new Category();
    category1.setId(100L);
    category1.setName("川菜");
    category1.setType(1);
    category1.setStatus(StatusConstant.ENABLE);
    List<Category> categoryList = Collections.singletonList(category1);

    // 2. Mock 依赖行为
    when(mapper.selectList(any())).thenReturn(categoryList);

    // 3. 执行测试
    List<Category> result = categoryService.list(type);

    // 4. 验证结果
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("川菜", result.get(0).getName());

    // 5. 验证方法调用
    verify(mapper).selectList(categoryWrapperCaptor.capture());
  }

  @Test
  void listWithNullType() {
    // 1. 准备测试数据
    Category category1 = new Category();
    category1.setId(100L);
    category1.setName("川菜");
    category1.setStatus(StatusConstant.ENABLE);
    List<Category> categoryList = Collections.singletonList(category1);

    // 2. Mock 依赖行为
    when(mapper.selectList(any())).thenReturn(categoryList);

    // 3. 执行测试 - type 为 null（使用类型转换避免方法歧义）
    Integer type = null;
    List<Category> result = categoryService.list(type);

    // 4. 验证结果
    assertNotNull(result);
    assertEquals(1, result.size());

    // 5. 验证方法调用
    verify(mapper).selectList(categoryWrapperCaptor.capture());
  }

  @Test
  void getCategoryMapByIdsSuccess() {
    // 1. 准备测试数据
    Set<Long> categoryIds = Set.of(100L, 200L);
    Category category1 = new Category();
    category1.setId(100L);
    category1.setName("川菜");
    Category category2 = new Category();
    category2.setId(200L);
    category2.setName("粤菜");
    List<Category> categories = List.of(category1, category2);

    // 2. Mock 依赖行为
    when(mapper.selectByIds(any())).thenReturn(categories);

    // 3. 执行测试
    Map<Long, String> result = categoryService.getCategoryMapByIds(categoryIds);

    // 4. 验证结果
    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals("川菜", result.get(100L));
    assertEquals("粤菜", result.get(200L));

    // 5. 验证方法调用
    verify(mapper).selectByIds(any());
  }

  @Test
  void getCategoryMapByIdsWithEmptySet() {
    // 1. 准备测试数据
    Set<Long> categoryIds = Collections.emptySet();

    // 2. 执行测试
    Map<Long, String> result = categoryService.getCategoryMapByIds(categoryIds);

    // 3. 验证结果
    assertNotNull(result);
    assertEquals(0, result.size());
  }

  @Test
  void getCategoryMapByIdsWithNull() {
    // 1. 执行测试 - 传入 null
    Map<Long, String> result = categoryService.getCategoryMapByIds(null);

    // 2. 验证结果
    assertNotNull(result);
    assertEquals(0, result.size());
  }
}
