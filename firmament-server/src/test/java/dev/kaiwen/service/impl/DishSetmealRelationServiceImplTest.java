package dev.kaiwen.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import dev.kaiwen.constant.StatusConstant;
import dev.kaiwen.entity.Dish;
import dev.kaiwen.entity.Setmeal;
import dev.kaiwen.entity.SetmealDish;
import dev.kaiwen.mapper.DishMapper;
import dev.kaiwen.mapper.SetmealDishMapper;
import dev.kaiwen.mapper.SetmealMapper;
import java.util.Collections;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DishSetmealRelationServiceImplTest {

  @InjectMocks
  private DishSetmealRelationServiceImpl relationService;

  @Mock
  private SetmealDishMapper setmealDishMapper;

  @Mock
  private DishMapper dishMapper;

  @Mock
  private SetmealMapper setmealMapper;

  @BeforeEach
  void setUp() {
    MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
    TableInfoHelper.initTableInfo(assistant, SetmealDish.class);
    TableInfoHelper.initTableInfo(assistant, Setmeal.class);
    TableInfoHelper.initTableInfo(assistant, Dish.class);
  }

  @Test
  void hasEnabledSetmealUsingDishReturnsFalseWhenNoRelations() {
    when(setmealDishMapper.selectList(any())).thenReturn(Collections.emptyList());

    boolean result = relationService.hasEnabledSetmealUsingDish(1L);

    assertFalse(result);
  }

  @Test
  void hasEnabledSetmealUsingDishReturnsFalseWhenRelationsNull() {
    when(setmealDishMapper.selectList(any())).thenReturn(null);

    boolean result = relationService.hasEnabledSetmealUsingDish(1L);

    assertFalse(result);
  }

  @Test
  void hasEnabledSetmealUsingDishReturnsFalseWhenRelationSetmealIdNull() {
    SetmealDish relation = new SetmealDish();
    relation.setSetmealId(null);

    when(setmealDishMapper.selectList(any())).thenReturn(List.of(relation));

    boolean result = relationService.hasEnabledSetmealUsingDish(4L);

    assertFalse(result);
  }

  @Test
  void hasEnabledSetmealUsingDishReturnsTrueWhenEnabledExists() {
    SetmealDish relation = new SetmealDish();
    relation.setSetmealId(10L);

    when(setmealDishMapper.selectList(any())).thenReturn(List.of(relation));

    Setmeal enabled = new Setmeal();
    enabled.setId(10L);
    enabled.setStatus(StatusConstant.ENABLE);
    when(setmealMapper.selectList(any())).thenReturn(List.of(enabled));

    boolean result = relationService.hasEnabledSetmealUsingDish(2L);

    assertTrue(result);
  }

  @Test
  void hasEnabledSetmealUsingDishReturnsFalseWhenAllDisabled() {
    SetmealDish relation = new SetmealDish();
    relation.setSetmealId(11L);

    when(setmealDishMapper.selectList(any())).thenReturn(List.of(relation));

    Setmeal disabled = new Setmeal();
    disabled.setId(11L);
    disabled.setStatus(StatusConstant.DISABLE);
    when(setmealMapper.selectList(any())).thenReturn(List.of(disabled));

    boolean result = relationService.hasEnabledSetmealUsingDish(3L);

    assertFalse(result);
  }

  @Test
  void hasDisabledDishInSetmealReturnsFalseWhenNoRelations() {
    when(setmealDishMapper.selectList(any())).thenReturn(Collections.emptyList());

    boolean result = relationService.hasDisabledDishInSetmeal(5L);

    assertFalse(result);
  }

  @Test
  void hasDisabledDishInSetmealReturnsFalseWhenRelationsNull() {
    when(setmealDishMapper.selectList(any())).thenReturn(null);

    boolean result = relationService.hasDisabledDishInSetmeal(5L);

    assertFalse(result);
  }

  @Test
  void hasDisabledDishInSetmealReturnsFalseWhenRelationDishIdNull() {
    SetmealDish relation = new SetmealDish();
    relation.setDishId(null);

    when(setmealDishMapper.selectList(any())).thenReturn(List.of(relation));

    boolean result = relationService.hasDisabledDishInSetmeal(7L);

    assertFalse(result);
  }

  @Test
  void hasDisabledDishInSetmealReturnsTrueWhenDisabledDishExists() {
    SetmealDish relation = new SetmealDish();
    relation.setDishId(21L);

    when(setmealDishMapper.selectList(any())).thenReturn(List.of(relation));

    Dish disabledDish = new Dish();
    disabledDish.setId(21L);
    disabledDish.setStatus(StatusConstant.DISABLE);
    when(dishMapper.selectList(any())).thenReturn(List.of(disabledDish));

    boolean result = relationService.hasDisabledDishInSetmeal(6L);

    assertTrue(result);
  }
}
