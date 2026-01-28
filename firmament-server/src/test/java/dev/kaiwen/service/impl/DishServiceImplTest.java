package dev.kaiwen.service.impl;

import static dev.kaiwen.constant.MessageConstant.DISH_BE_RELATED_BY_SETMEAL;
import static dev.kaiwen.constant.MessageConstant.DISH_DISABLE_FAILED;
import static dev.kaiwen.constant.MessageConstant.DISH_ON_SALE;
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
import dev.kaiwen.dto.DishDto;
import dev.kaiwen.dto.DishPageQueryDto;
import dev.kaiwen.entity.Dish;
import dev.kaiwen.entity.DishFlavor;
import dev.kaiwen.entity.SetmealDish;
import dev.kaiwen.exception.DeletionNotAllowedException;
import dev.kaiwen.exception.DishDisableFailedException;
import dev.kaiwen.mapper.DishFlavorMapper;
import dev.kaiwen.mapper.DishMapper;
import dev.kaiwen.mapper.SetmealDishMapper;
import dev.kaiwen.result.PageResult;
import dev.kaiwen.service.CategoryService;
import dev.kaiwen.service.DishFlavorService;
import dev.kaiwen.service.DishSetmealRelationService;
import dev.kaiwen.vo.DishVo;
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
class DishServiceImplTest {

  @InjectMocks
  private DishServiceImpl dishService;

  @Mock
  private DishMapper mapper;

  @Mock
  private DishFlavorMapper dishFlavorMapper;

  @Mock
  private SetmealDishMapper setmealDishMapper;

  @Mock
  private DishFlavorService dishFlavorService;

  @Mock
  private CategoryService categoryService;

  @Mock
  private DishSetmealRelationService dishSetmealRelationService;

  @Captor
  private ArgumentCaptor<List<DishFlavor>> flavorListCaptor;

  @Captor
  private ArgumentCaptor<LambdaQueryWrapper<Dish>> dishWrapperCaptor;

  @BeforeEach
  void setUp() {
    MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
    TableInfoHelper.initTableInfo(assistant, Dish.class);
    TableInfoHelper.initTableInfo(assistant, DishFlavor.class);
    TableInfoHelper.initTableInfo(assistant, SetmealDish.class);
    ReflectionTestUtils.setField(dishService, "baseMapper", mapper);
  }

  @Test
  void saveWithFlavorSavesFlavors() {
    DishFlavor flavor1 = new DishFlavor();
    flavor1.setName("辣度");
    flavor1.setValue("微辣");
    DishFlavor flavor2 = new DishFlavor();
    flavor2.setName("辣度");
    flavor2.setValue("中辣");

    DishDto dishDto = new DishDto();
    dishDto.setName("宫保鸡丁");
    dishDto.setFlavors(List.of(flavor1, flavor2));

    doAnswer(invocation -> {
      Dish dish = invocation.getArgument(0);
      dish.setId(10L);
      return 1;
    }).when(mapper).insert(any(Dish.class));

    dishService.saveWithFlavor(dishDto);

    verify(dishFlavorService).saveBatch(flavorListCaptor.capture());
    List<DishFlavor> savedFlavors = flavorListCaptor.getValue();
    assertEquals(2, savedFlavors.size());
    assertTrue(savedFlavors.stream().allMatch(f -> 10L == f.getDishId()));
  }

  @Test
  void saveWithFlavorWithEmptyFlavors() {
    DishDto dishDto = new DishDto();
    dishDto.setName("红烧肉");
    dishDto.setFlavors(Collections.emptyList());

    doAnswer(invocation -> {
      Dish dish = invocation.getArgument(0);
      dish.setId(20L);
      return 1;
    }).when(mapper).insert(any(Dish.class));

    dishService.saveWithFlavor(dishDto);

    verify(dishFlavorService, never()).saveBatch(any());
  }

  @Test
  void saveWithFlavorWithNullFlavors() {
    DishDto dishDto = new DishDto();
    dishDto.setName("水煮鱼");
    dishDto.setFlavors(null);

    doAnswer(invocation -> {
      Dish dish = invocation.getArgument(0);
      dish.setId(21L);
      return 1;
    }).when(mapper).insert(any(Dish.class));

    dishService.saveWithFlavor(dishDto);

    verify(dishFlavorService, never()).saveBatch(any());
  }

  @Test
  void pageQuerySuccess() {
    DishPageQueryDto dto = new DishPageQueryDto();
    dto.setPage(1);
    dto.setPageSize(10);
    dto.setName("鸡");

    Dish dish1 = new Dish();
    dish1.setId(1L);
    dish1.setName("宫保鸡丁");
    dish1.setCategoryId(100L);
    Dish dish2 = new Dish();
    dish2.setId(2L);
    dish2.setName("辣子鸡");
    dish2.setCategoryId(200L);
    List<Dish> records = List.of(dish1, dish2);

    doAnswer(invocation -> {
      Page<Dish> pageArg = invocation.getArgument(0);
      pageArg.setRecords(records);
      pageArg.setTotal(2L);
      return pageArg;
    }).when(mapper).selectPage(any(), any());

    when(categoryService.getCategoryMapByIds(any())).thenReturn(
        Map.of(100L, "川菜", 200L, "湘菜"));

    PageResult result = dishService.pageQuery(dto);

    assertEquals(2L, result.getTotal());
    @SuppressWarnings("unchecked")
    List<DishVo> voList = (List<DishVo>) result.getRecords();
    assertEquals(2, voList.size());
    assertEquals("川菜", voList.get(0).getCategoryName());
  }

  @Test
  void pageQueryWithCategoryAndStatus() {
    DishPageQueryDto dto = new DishPageQueryDto();
    dto.setPage(1);
    dto.setPageSize(5);
    dto.setName("");
    dto.setCategoryId(300);
    dto.setStatus(1);

    Dish dish = new Dish();
    dish.setId(30L);
    dish.setName("酸菜鱼");
    dish.setCategoryId(300L);
    List<Dish> records = List.of(dish);

    doAnswer(invocation -> {
      Page<Dish> pageArg = invocation.getArgument(0);
      pageArg.setRecords(records);
      pageArg.setTotal(1L);
      return pageArg;
    }).when(mapper).selectPage(any(), any());

    when(categoryService.getCategoryMapByIds(any())).thenReturn(Map.of(300L, "江浙菜"));

    PageResult result = dishService.pageQuery(dto);

    assertEquals(1L, result.getTotal());
    @SuppressWarnings("unchecked")
    List<DishVo> voList = (List<DishVo>) result.getRecords();
    assertEquals("江浙菜", voList.get(0).getCategoryName());
  }

  @Test
  void deleteDishOnSaleThrows() {
    when(mapper.selectCount(any())).thenReturn(1L);

    List<Long> ids = List.of(1L);
    DeletionNotAllowedException exception = assertThrows(DeletionNotAllowedException.class,
        () -> dishService.deleteDish(ids));

    assertEquals(DISH_ON_SALE, exception.getMessage());
  }

  @Test
  void deleteDishRelatedSetmealThrows() {
    when(mapper.selectCount(any())).thenReturn(0L);
    when(setmealDishMapper.selectCount(any())).thenReturn(1L);

    List<Long> ids = List.of(2L);
    DeletionNotAllowedException exception = assertThrows(DeletionNotAllowedException.class,
        () -> dishService.deleteDish(ids));

    assertEquals(DISH_BE_RELATED_BY_SETMEAL, exception.getMessage());
  }

  @Test
  void deleteDishSuccess() {
    when(mapper.selectCount(any())).thenReturn(0L);
    when(setmealDishMapper.selectCount(any())).thenReturn(0L);
    when(mapper.deleteByIds(any())).thenReturn(1);
    when(dishFlavorMapper.delete(any())).thenReturn(1);

    dishService.deleteDish(List.of(3L, 4L));

    verify(mapper).deleteByIds(List.of(3L, 4L));
    verify(dishFlavorMapper).delete(any());
  }

  @Test
  void getDishByIdReturnsFlavors() {
    Dish dish = new Dish();
    dish.setId(5L);
    dish.setName("鱼香肉丝");

    DishFlavor flavor = new DishFlavor();
    flavor.setDishId(5L);
    flavor.setName("辣度");

    when(mapper.selectById(5L)).thenReturn(dish);
    when(dishFlavorMapper.selectList(any())).thenReturn(List.of(flavor));

    DishVo result = dishService.getDishById(5L);

    assertNotNull(result);
    assertEquals(1, result.getFlavors().size());
  }

  @Test
  void updateDishWithFlavors() {
    DishFlavor flavor = new DishFlavor();
    flavor.setName("甜度");

    DishDto dishDto = new DishDto();
    dishDto.setId(6L);
    dishDto.setName("红烧排骨");
    dishDto.setFlavors(List.of(flavor));

    when(mapper.updateById(any(Dish.class))).thenReturn(1);
    when(dishFlavorMapper.delete(any())).thenReturn(1);

    dishService.updateDish(dishDto);

    verify(dishFlavorService).saveBatch(flavorListCaptor.capture());
    List<DishFlavor> savedFlavors = flavorListCaptor.getValue();
    assertEquals(6L, savedFlavors.get(0).getDishId());
  }

  @Test
  void updateDishWithoutFlavors() {
    DishDto dishDto = new DishDto();
    dishDto.setId(7L);
    dishDto.setName("回锅肉");
    dishDto.setFlavors(Collections.emptyList());

    when(mapper.updateById(any(Dish.class))).thenReturn(1);
    when(dishFlavorMapper.delete(any())).thenReturn(1);

    dishService.updateDish(dishDto);

    verify(dishFlavorService, never()).saveBatch(any());
  }

  @Test
  void updateDishWithNullFlavors() {
    DishDto dishDto = new DishDto();
    dishDto.setId(8L);
    dishDto.setName("剁椒鱼头");
    dishDto.setFlavors(null);

    when(mapper.updateById(any(Dish.class))).thenReturn(1);
    when(dishFlavorMapper.delete(any())).thenReturn(1);

    dishService.updateDish(dishDto);

    verify(dishFlavorService, never()).saveBatch(any());
  }

  @Test
  void listWithFlavorReturnsEmptyWhenNoData() {
    Dish dish = new Dish();
    dish.setCategoryId(1L);

    when(mapper.selectList(any())).thenReturn(Collections.emptyList());

    List<DishVo> result = dishService.listWithFlavor(dish);

    assertTrue(result.isEmpty());
    verify(mapper).selectList(dishWrapperCaptor.capture());
  }

  @Test
  void listWithFlavorReturnsEmptyWhenNullList() {
    Dish dish = new Dish();
    dish.setCategoryId(2L);
    dish.setStatus(1);

    when(mapper.selectList(any())).thenReturn(null);

    List<DishVo> result = dishService.listWithFlavor(dish);

    assertTrue(result.isEmpty());
  }

  @Test
  void listWithFlavorWithoutCategoryIdUsesStatus() {
    Dish dish = new Dish();
    dish.setCategoryId(null);
    dish.setStatus(1);

    Dish dish1 = new Dish();
    dish1.setId(3L);
    dish1.setName("鱼香茄子");
    Dish dish2 = new Dish();
    dish2.setId(4L);
    dish2.setName("西红柿炒蛋");
    List<Dish> dishList = List.of(dish1, dish2);

    DishFlavor flavor = new DishFlavor();
    flavor.setDishId(3L);
    flavor.setName("辣度");

    when(mapper.selectList(any())).thenReturn(dishList);
    when(dishFlavorMapper.selectList(any())).thenReturn(List.of(flavor));

    List<DishVo> result = dishService.listWithFlavor(dish);

    assertEquals(2, result.size());
    assertNotNull(result.get(0).getFlavors());
  }

  @Test
  void listWithFlavorSuccess() {
    Dish dish = new Dish();
    dish.setCategoryId(1L);

    Dish dish1 = new Dish();
    dish1.setId(1L);
    dish1.setName("番茄炒蛋");
    Dish dish2 = new Dish();
    dish2.setId(2L);
    dish2.setName("青椒肉丝");
    List<Dish> dishList = List.of(dish1, dish2);

    DishFlavor flavor = new DishFlavor();
    flavor.setDishId(1L);
    flavor.setName("辣度");

    when(mapper.selectList(any())).thenReturn(dishList);
    when(dishFlavorMapper.selectList(any())).thenReturn(List.of(flavor));

    List<DishVo> result = dishService.listWithFlavor(dish);

    assertEquals(2, result.size());
    assertNotNull(result.get(0).getFlavors());
  }

  @Test
  void startOrStopDisableWithRelationThrows() {
    when(dishSetmealRelationService.hasEnabledSetmealUsingDish(8L)).thenReturn(true);

    DishDisableFailedException exception = assertThrows(DishDisableFailedException.class,
        () -> dishService.startOrStop(StatusConstant.DISABLE, 8L));

    assertEquals(DISH_DISABLE_FAILED, exception.getMessage());
  }

  @Test
  void startOrStopDisableWithoutRelationUpdates() {
    when(dishSetmealRelationService.hasEnabledSetmealUsingDish(10L)).thenReturn(false);
    when(mapper.update(any(), any())).thenReturn(1);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(100L);

      dishService.startOrStop(StatusConstant.DISABLE, 10L);

      verify(mapper).update(any(), any());
    }
  }

  @Test
  void startOrStopUpdates() {
    when(mapper.update(any(), any())).thenReturn(1);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(99L);

      dishService.startOrStop(StatusConstant.ENABLE, 9L);

      verify(mapper).update(any(), any());
    }
  }
}
