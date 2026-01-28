package dev.kaiwen.service.impl;

import static dev.kaiwen.constant.MessageConstant.SETMEAL_ENABLE_FAILED;
import static dev.kaiwen.constant.MessageConstant.SETMEAL_ON_SALE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import dev.kaiwen.constant.StatusConstant;
import dev.kaiwen.context.BaseContext;
import dev.kaiwen.dto.SetmealDto;
import dev.kaiwen.dto.SetmealPageQueryDto;
import dev.kaiwen.entity.Dish;
import dev.kaiwen.entity.Setmeal;
import dev.kaiwen.entity.SetmealDish;
import dev.kaiwen.exception.DeletionNotAllowedException;
import dev.kaiwen.exception.SetmealEnableFailedException;
import dev.kaiwen.mapper.DishMapper;
import dev.kaiwen.mapper.SetmealDishMapper;
import dev.kaiwen.mapper.SetmealMapper;
import dev.kaiwen.result.PageResult;
import dev.kaiwen.service.CategoryService;
import dev.kaiwen.service.DishSetmealRelationService;
import dev.kaiwen.service.SetmealDishService;
import dev.kaiwen.vo.DishItemVo;
import dev.kaiwen.vo.SetmealVo;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
class SetmealServiceImplTest {

  @InjectMocks
  private SetmealServiceImpl setmealService;

  @Mock
  private SetmealMapper mapper;

  @Mock
  private SetmealDishMapper setmealDishMapper;

  @Mock
  private SetmealDishService setmealDishService;

  @Mock
  private CategoryService categoryService;

  @Mock
  private DishSetmealRelationService dishSetmealRelationService;

  @Mock
  private DishMapper dishMapper;

  @Captor
  private ArgumentCaptor<List<SetmealDish>> setmealDishCaptor;

  @Captor
  private ArgumentCaptor<LambdaQueryWrapper<Setmeal>> setmealWrapperCaptor;

  @BeforeEach
  void setUp() {
    MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
    TableInfoHelper.initTableInfo(assistant, Setmeal.class);
    TableInfoHelper.initTableInfo(assistant, SetmealDish.class);
    TableInfoHelper.initTableInfo(assistant, Dish.class);
    ReflectionTestUtils.setField(setmealService, "baseMapper", mapper);
  }

  @Test
  void saveWithDishSavesRelations() {
    SetmealDish relation = new SetmealDish();
    relation.setDishId(1L);

    SetmealDto dto = new SetmealDto();
    dto.setName("超值套餐");
    dto.setSetmealDishes(List.of(relation));

    doAnswer(invocation -> {
      Setmeal setmeal = invocation.getArgument(0);
      setmeal.setId(10L);
      return 1;
    }).when(mapper).insert(any(Setmeal.class));

    setmealService.saveWithDish(dto);

    verify(setmealDishService).saveBatch(setmealDishCaptor.capture());
    List<SetmealDish> saved = setmealDishCaptor.getValue();
    assertEquals(10L, saved.get(0).getSetmealId());
  }

  @Test
  void saveWithDishWithoutRelations() {
    SetmealDto dto = new SetmealDto();
    dto.setName("纯套餐");
    dto.setSetmealDishes(Collections.emptyList());

    doAnswer(invocation -> {
      Setmeal setmeal = invocation.getArgument(0);
      setmeal.setId(11L);
      return 1;
    }).when(mapper).insert(any(Setmeal.class));

    setmealService.saveWithDish(dto);

    verify(setmealDishService, never()).saveBatch(any());
  }

  @Test
  void saveWithDishWithNullRelations() {
    SetmealDto dto = new SetmealDto();
    dto.setName("空关系套餐");
    dto.setSetmealDishes(null);

    doAnswer(invocation -> {
      Setmeal setmeal = invocation.getArgument(0);
      setmeal.setId(12L);
      return 1;
    }).when(mapper).insert(any(Setmeal.class));

    setmealService.saveWithDish(dto);

    verify(setmealDishService, never()).saveBatch(any());
  }

  @Test
  void listSuccess() {
    Setmeal setmeal = new Setmeal();
    setmeal.setCategoryId(1L);

    when(mapper.selectList(any())).thenReturn(List.of(new Setmeal()));

    List<Setmeal> result = setmealService.list(setmeal);

    assertEquals(1, result.size());
    verify(mapper).selectList(setmealWrapperCaptor.capture());
  }

  @Test
  void listWithStatusAndName() {
    Setmeal setmeal = new Setmeal();
    setmeal.setStatus(1);
    setmeal.setName("套餐");

    when(mapper.selectList(any())).thenReturn(List.of(new Setmeal()));

    List<Setmeal> result = setmealService.list(setmeal);

    assertEquals(1, result.size());
    verify(mapper).selectList(setmealWrapperCaptor.capture());
  }

  @Test
  void getDishItemByIdReturnsEmptyWhenNoRelations() {
    when(setmealDishMapper.selectList(any())).thenReturn(Collections.emptyList());

    List<DishItemVo> result = setmealService.getDishItemById(1L);

    assertTrue(result.isEmpty());
  }

  @Test
  void getDishItemByIdReturnsEmptyWhenRelationsNull() {
    when(setmealDishMapper.selectList(any())).thenReturn(null);

    List<DishItemVo> result = setmealService.getDishItemById(1L);

    assertTrue(result.isEmpty());
  }

  @Test
  void getDishItemByIdReturnsEmptyWhenDishIdsEmpty() {
    SetmealDish relation = new SetmealDish();
    relation.setDishId(null);

    when(setmealDishMapper.selectList(any())).thenReturn(List.of(relation));

    List<DishItemVo> result = setmealService.getDishItemById(2L);

    assertTrue(result.isEmpty());
    verify(dishMapper, never()).selectList(any());
  }

  @Test
  void getDishItemByIdMapsCopies() {
    SetmealDish relation = new SetmealDish();
    relation.setDishId(2L);
    relation.setCopies(3);

    when(setmealDishMapper.selectList(any())).thenReturn(List.of(relation));

    Dish dish = new Dish();
    dish.setId(2L);
    dish.setName("酸菜鱼");
    dish.setImage("img");
    dish.setDescription("desc");
    when(dishMapper.selectList(any())).thenReturn(List.of(dish));

    List<DishItemVo> result = setmealService.getDishItemById(2L);

    assertEquals(1, result.size());
    assertEquals(3, result.get(0).getCopies());
  }

  @Test
  void getDishItemByIdKeepsFirstOnDuplicateDishId() {
    SetmealDish first = new SetmealDish();
    first.setDishId(5L);
    first.setCopies(2);
    SetmealDish second = new SetmealDish();
    second.setDishId(5L);
    second.setCopies(6);

    when(setmealDishMapper.selectList(any())).thenReturn(List.of(first, second));

    Dish dish = new Dish();
    dish.setId(5L);
    dish.setName("麻婆豆腐");
    when(dishMapper.selectList(any())).thenReturn(List.of(dish));

    List<DishItemVo> result = setmealService.getDishItemById(5L);

    assertEquals(1, result.size());
    assertEquals(2, result.get(0).getCopies());
  }

  @Test
  void getDishItemByIdUsesDefaultCopiesWhenRelationMissing() {
    SetmealDish relation = new SetmealDish();
    relation.setDishId(6L);
    relation.setCopies(4);

    when(setmealDishMapper.selectList(any())).thenReturn(List.of(relation));

    Dish dish = new Dish();
    dish.setId(9L);
    dish.setName("红烧肉");
    when(dishMapper.selectList(any())).thenReturn(List.of(dish));

    List<DishItemVo> result = setmealService.getDishItemById(6L);

    assertEquals(1, result.size());
    assertEquals(1, result.get(0).getCopies());
  }

  @Test
  void pageQuerySuccess() {
    SetmealPageQueryDto dto = new SetmealPageQueryDto();
    dto.setPage(1);
    dto.setPageSize(10);

    Setmeal setmealRecord = new Setmeal();
    setmealRecord.setId(1L);
    setmealRecord.setName("双人餐");
    setmealRecord.setCategoryId(100L);

    doAnswer(invocation -> {
      Page<Setmeal> pageArg = invocation.getArgument(0);
      pageArg.setRecords(List.of(setmealRecord));
      pageArg.setTotal(1L);
      return pageArg;
    }).when(mapper).selectPage(any(), any());

    when(categoryService.getCategoryMapByIds(any())).thenReturn(Map.of(100L, "套餐"));

    PageResult result = setmealService.pageQuery(dto);

    assertEquals(1L, result.getTotal());
    @SuppressWarnings("unchecked")
    List<SetmealVo> voList = (List<SetmealVo>) result.getRecords();
    assertEquals("套餐", voList.get(0).getCategoryName());
  }

  @Test
  void pageQueryWithCategoryAndStatus() {
    SetmealPageQueryDto dto = new SetmealPageQueryDto();
    dto.setPage(1);
    dto.setPageSize(5);
    dto.setCategoryId(200);
    dto.setStatus(1);
    dto.setName("");

    Setmeal setmealRecord = new Setmeal();
    setmealRecord.setId(2L);
    setmealRecord.setName("工作餐");
    setmealRecord.setCategoryId(200L);

    doAnswer(invocation -> {
      Page<Setmeal> pageArg = invocation.getArgument(0);
      pageArg.setRecords(List.of(setmealRecord));
      pageArg.setTotal(1L);
      return pageArg;
    }).when(mapper).selectPage(any(), any());

    when(categoryService.getCategoryMapByIds(any())).thenReturn(Map.of(200L, "午餐"));

    PageResult result = setmealService.pageQuery(dto);

    assertEquals(1L, result.getTotal());
    @SuppressWarnings("unchecked")
    List<SetmealVo> voList = (List<SetmealVo>) result.getRecords();
    assertEquals("午餐", voList.get(0).getCategoryName());
  }

  @Test
  void getByIdWithDishSuccess() {
    Setmeal setmeal = new Setmeal();
    setmeal.setId(3L);
    setmeal.setName("三人餐");

    SetmealDish relation = new SetmealDish();
    relation.setSetmealId(3L);
    relation.setDishId(30L);

    when(mapper.selectById(3L)).thenReturn(setmeal);
    when(setmealDishMapper.selectList(any())).thenReturn(List.of(relation));

    SetmealVo result = setmealService.getByIdWithDish(3L);

    assertNotNull(result);
    assertEquals(1, result.getSetmealDishes().size());
  }

  @Test
  void updateWithRelations() {
    SetmealDish relation = new SetmealDish();
    relation.setDishId(40L);

    SetmealDto dto = new SetmealDto();
    dto.setId(4L);
    dto.setName("更新套餐");
    dto.setSetmealDishes(List.of(relation));

    when(mapper.updateById(any(Setmeal.class))).thenReturn(1);
    when(setmealDishMapper.delete(any())).thenReturn(1);

    setmealService.update(dto);

    verify(setmealDishService).saveBatch(setmealDishCaptor.capture());
    List<SetmealDish> saved = setmealDishCaptor.getValue();
    assertEquals(4L, saved.get(0).getSetmealId());
  }

  @Test
  void updateWithoutRelations() {
    SetmealDto dto = new SetmealDto();
    dto.setId(5L);
    dto.setName("更新套餐");
    dto.setSetmealDishes(Collections.emptyList());

    when(mapper.updateById(any(Setmeal.class))).thenReturn(1);
    when(setmealDishMapper.delete(any())).thenReturn(1);

    setmealService.update(dto);

    verify(setmealDishService, never()).saveBatch(any());
  }

  @Test
  void updateWithNullRelations() {
    SetmealDto dto = new SetmealDto();
    dto.setId(6L);
    dto.setName("更新套餐");
    dto.setSetmealDishes(null);

    when(mapper.updateById(any(Setmeal.class))).thenReturn(1);
    when(setmealDishMapper.delete(any())).thenReturn(1);

    setmealService.update(dto);

    verify(setmealDishService, never()).saveBatch(any());
  }

  @Test
  void deleteBatchOnSaleThrows() {
    when(mapper.selectCount(any())).thenReturn(1L);

    List<Long> ids = List.of(6L);
    DeletionNotAllowedException exception = assertThrows(DeletionNotAllowedException.class,
        () -> setmealService.deleteBatch(ids));

    assertEquals(SETMEAL_ON_SALE, exception.getMessage());
  }

  @Test
  void deleteBatchSuccess() {
    when(mapper.selectCount(any())).thenReturn(0L);
    when(setmealDishMapper.delete(any())).thenReturn(1);
    when(mapper.deleteByIds(any())).thenReturn(1);

    setmealService.deleteBatch(List.of(7L, 8L));

    verify(setmealDishMapper).delete(any());
    verify(mapper).deleteByIds(List.of(7L, 8L));
  }

  @Test
  void startOrStopEnableWithDisabledDishThrows() {
    when(dishSetmealRelationService.hasDisabledDishInSetmeal(9L)).thenReturn(true);

    SetmealEnableFailedException exception = assertThrows(SetmealEnableFailedException.class,
        () -> setmealService.startOrStop(StatusConstant.ENABLE, 9L));

    assertEquals(SETMEAL_ENABLE_FAILED, exception.getMessage());
  }

  @Test
  void startOrStopUpdates() {
    when(mapper.update(any(), any())).thenReturn(1);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(101L);

      setmealService.startOrStop(StatusConstant.DISABLE, 10L);

      verify(mapper).update(any(), any());
    }
  }

  @Test
  void startOrStopEnableWithoutDisabledDishUpdates() {
    when(dishSetmealRelationService.hasDisabledDishInSetmeal(11L)).thenReturn(false);
    when(mapper.update(any(), any())).thenReturn(1);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(102L);

      setmealService.startOrStop(StatusConstant.ENABLE, 11L);

      verify(mapper).update(any(), any());
    }
  }
}
