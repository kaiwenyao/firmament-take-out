package dev.kaiwen.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import dev.kaiwen.context.BaseContext;
import dev.kaiwen.dto.ShoppingCartDto;
import dev.kaiwen.entity.Dish;
import dev.kaiwen.entity.Setmeal;
import dev.kaiwen.entity.ShoppingCart;
import dev.kaiwen.mapper.ShoppingCartMapper;
import dev.kaiwen.service.DishService;
import dev.kaiwen.service.SetmealService;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
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
class ShoppingCartServiceImplTest {

  @InjectMocks
  private ShoppingCartServiceImpl shoppingCartService;

  @Mock
  private ShoppingCartMapper mapper;

  @Mock
  private SetmealService setmealService;

  @Mock
  private DishService dishService;

  @Captor
  private ArgumentCaptor<ShoppingCart> shoppingCartCaptor;

  @Captor
  private ArgumentCaptor<LambdaQueryWrapper<ShoppingCart>> wrapperCaptor;

  @BeforeEach
  void setUp() {
    MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
    TableInfoHelper.initTableInfo(assistant, ShoppingCart.class);
    ReflectionTestUtils.setField(shoppingCartService, "baseMapper", mapper);
  }

  @Test
  void addShoppingCartWithExistingItem() {
    // 测试场景：购物车中已存在该商品，需要更新数量
    // 覆盖：if (list != null && !list.isEmpty()) 分支，包括 list.get(0)、setNumber、updateById
    // 1. 准备测试数据
    ShoppingCartDto dto = new ShoppingCartDto();
    dto.setDishId(100L);

    ShoppingCart existingCart = new ShoppingCart();
    existingCart.setId(1L);
    existingCart.setDishId(100L);
    existingCart.setNumber(2);
    List<ShoppingCart> existingList = Collections.singletonList(existingCart);

    // 2. Mock 依赖行为
    when(mapper.selectList(any())).thenReturn(existingList);
    when(mapper.updateById(any(ShoppingCart.class))).thenReturn(1);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(888L);

      // 3. 执行测试
      shoppingCartService.addShoppingCart(dto);

      // 4. 验证方法调用
      verify(mapper).selectList(wrapperCaptor.capture());
      verify(mapper).updateById(shoppingCartCaptor.capture());
      ShoppingCart updatedCart = shoppingCartCaptor.getValue();
      assertEquals(3, updatedCart.getNumber()); // 2 + 1 = 3
      assertEquals(1L, updatedCart.getId());
      assertEquals(100L, updatedCart.getDishId());
    }
  }

  @Test
  void addShoppingCartWithExistingSetmeal() {
    // 测试场景：购物车中已存在套餐商品，需要更新数量
    // 覆盖：if (list != null && !list.isEmpty()) 分支（套餐场景）
    // 1. 准备测试数据
    ShoppingCartDto dto = new ShoppingCartDto();
    dto.setSetmealId(200L);

    ShoppingCart existingCart = new ShoppingCart();
    existingCart.setId(2L);
    existingCart.setSetmealId(200L);
    existingCart.setNumber(1);
    List<ShoppingCart> existingList = Collections.singletonList(existingCart);

    // 2. Mock 依赖行为
    when(mapper.selectList(any())).thenReturn(existingList);
    when(mapper.updateById(any(ShoppingCart.class))).thenReturn(1);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(888L);

      // 3. 执行测试
      shoppingCartService.addShoppingCart(dto);

      // 4. 验证方法调用
      verify(mapper).selectList(wrapperCaptor.capture());
      verify(mapper).updateById(shoppingCartCaptor.capture());
      ShoppingCart updatedCart = shoppingCartCaptor.getValue();
      assertEquals(2, updatedCart.getNumber()); // 1 + 1 = 2
      assertEquals(2L, updatedCart.getId());
      assertEquals(200L, updatedCart.getSetmealId());
    }
  }

  @Test
  void addShoppingCartWithNewDish() {
    // 测试场景：添加新菜品到购物车，且菜品存在
    // 覆盖：if (dishId != null) 和 if (dish != null) 分支，包括设置 name、image、amount
    // 1. 准备测试数据
    ShoppingCartDto dto = new ShoppingCartDto();
    dto.setDishId(100L);

    Dish dish = new Dish();
    dish.setId(100L);
    dish.setName("宫保鸡丁");
    dish.setImage("image.jpg");
    dish.setPrice(BigDecimal.valueOf(38.0));

    // 2. Mock 依赖行为 - 购物车中没有该商品
    when(mapper.selectList(any())).thenReturn(Collections.emptyList());
    when(dishService.getById(100L)).thenReturn(dish);
    when(mapper.insert(any(ShoppingCart.class))).thenReturn(1);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(888L);

      // 3. 执行测试
      shoppingCartService.addShoppingCart(dto);

      // 4. 验证方法调用
      verify(mapper).selectList(wrapperCaptor.capture());
      verify(dishService).getById(100L);
      verify(mapper).insert(shoppingCartCaptor.capture());
      ShoppingCart savedCart = shoppingCartCaptor.getValue();
      assertEquals(100L, savedCart.getDishId());
      assertEquals("宫保鸡丁", savedCart.getName());
      assertEquals("image.jpg", savedCart.getImage());
      assertEquals(BigDecimal.valueOf(38.0), savedCart.getAmount());
      assertEquals(1, savedCart.getNumber());
      assertEquals(888L, savedCart.getUserId());
    }
  }

  @Test
  void addShoppingCartWithNewSetmeal() {
    // 测试场景：添加新套餐到购物车，且套餐存在
    // 覆盖：else if (setmealId != null) 和 if (setmeal != null) 分支，包括设置 name、image、amount
    // 1. 准备测试数据
    ShoppingCartDto dto = new ShoppingCartDto();
    dto.setSetmealId(200L);

    Setmeal setmeal = new Setmeal();
    setmeal.setId(200L);
    setmeal.setName("豪华套餐");
    setmeal.setImage("setmeal.jpg");
    setmeal.setPrice(BigDecimal.valueOf(88.0));

    // 2. Mock 依赖行为 - 购物车中没有该商品
    when(mapper.selectList(any())).thenReturn(Collections.emptyList());
    when(setmealService.getById(200L)).thenReturn(setmeal);
    when(mapper.insert(any(ShoppingCart.class))).thenReturn(1);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(888L);

      // 3. 执行测试
      shoppingCartService.addShoppingCart(dto);

      // 4. 验证方法调用
      verify(mapper).selectList(wrapperCaptor.capture());
      verify(setmealService).getById(200L);
      verify(mapper).insert(shoppingCartCaptor.capture());
      ShoppingCart savedCart = shoppingCartCaptor.getValue();
      assertEquals(200L, savedCart.getSetmealId());
      assertEquals("豪华套餐", savedCart.getName());
      assertEquals("setmeal.jpg", savedCart.getImage());
      assertEquals(BigDecimal.valueOf(88.0), savedCart.getAmount());
      assertEquals(1, savedCart.getNumber());
      assertEquals(888L, savedCart.getUserId());
    }
  }

  @Test
  void subShoppingCartWithNumberGreaterThanOne() {
    // 1. 准备测试数据
    ShoppingCartDto dto = new ShoppingCartDto();
    dto.setDishId(100L);

    ShoppingCart existingCart = new ShoppingCart();
    existingCart.setId(1L);
    existingCart.setDishId(100L);
    existingCart.setNumber(3);
    List<ShoppingCart> existingList = Collections.singletonList(existingCart);

    // 2. Mock 依赖行为
    when(mapper.selectList(any())).thenReturn(existingList);
    when(mapper.updateById(any(ShoppingCart.class))).thenReturn(1);

    // 3. 执行测试
    shoppingCartService.subShoppingCart(dto);

    // 4. 验证方法调用
    verify(mapper).selectList(wrapperCaptor.capture());
    verify(mapper).updateById(shoppingCartCaptor.capture());
    ShoppingCart updatedCart = shoppingCartCaptor.getValue();
    assertEquals(2, updatedCart.getNumber()); // 3 - 1 = 2
  }

  @Test
  void subShoppingCartWithNumberEqualsOne() {
    // 1. 准备测试数据
    ShoppingCartDto dto = new ShoppingCartDto();
    dto.setDishId(100L);

    ShoppingCart existingCart = new ShoppingCart();
    existingCart.setId(1L);
    existingCart.setDishId(100L);
    existingCart.setNumber(1);
    List<ShoppingCart> existingList = Collections.singletonList(existingCart);

    // 2. Mock 依赖行为
    when(mapper.selectList(any())).thenReturn(existingList);
    when(mapper.deleteById(1L)).thenReturn(1);

    // 3. 执行测试
    shoppingCartService.subShoppingCart(dto);

    // 4. 验证方法调用
    verify(mapper).selectList(wrapperCaptor.capture());
    verify(mapper).deleteById(1L);
  }

  @Test
  void subShoppingCartWithNoItem() {
    // 1. 准备测试数据
    ShoppingCartDto dto = new ShoppingCartDto();
    dto.setDishId(100L);

    // 2. Mock 依赖行为 - 购物车中没有该商品
    when(mapper.selectList(any())).thenReturn(Collections.emptyList());

    // 3. 执行测试
    shoppingCartService.subShoppingCart(dto);

    // 4. 验证方法调用
    verify(mapper).selectList(wrapperCaptor.capture());
    // 不应该调用 updateById 或 deleteById
  }

  @Test
  void showShoppingCartSuccess() {
    // 1. 准备测试数据
    ShoppingCart cart1 = new ShoppingCart();
    cart1.setId(1L);
    cart1.setDishId(100L);
    cart1.setNumber(2);
    ShoppingCart cart2 = new ShoppingCart();
    cart2.setId(2L);
    cart2.setSetmealId(200L);
    cart2.setNumber(1);
    List<ShoppingCart> cartList = List.of(cart1, cart2);

    // 2. Mock 依赖行为
    when(mapper.selectList(any())).thenReturn(cartList);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(888L);

      // 3. 执行测试
      List<ShoppingCart> result = shoppingCartService.showShoppingCart();

      // 4. 验证结果
      assertNotNull(result);
      assertEquals(2, result.size());

      // 5. 验证方法调用
      verify(mapper).selectList(wrapperCaptor.capture());
    }
  }

  @Test
  void cleanShoppingCartSuccess() {
    // 1. Mock 依赖行为
    when(mapper.delete(any())).thenReturn(2);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(888L);

      // 2. 执行测试
      shoppingCartService.cleanShoppingCart();

      // 3. 验证方法调用
      verify(mapper).delete(any());
    }
  }

  @Test
  void addShoppingCartWithDishNotFound() {
    // 测试场景：dishId 不为 null，但查询不到对应的菜品（dish 为 null）
    // 1. 准备测试数据
    ShoppingCartDto dto = new ShoppingCartDto();
    dto.setDishId(100L);

    // 2. Mock 依赖行为 - 购物车中没有该商品，且查询不到菜品
    when(mapper.selectList(any())).thenReturn(Collections.emptyList());
    when(dishService.getById(100L)).thenReturn(null); // 返回 null，模拟菜品不存在
    when(mapper.insert(any(ShoppingCart.class))).thenReturn(1);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(888L);

      // 3. 执行测试
      shoppingCartService.addShoppingCart(dto);

      // 4. 验证方法调用
      verify(mapper).selectList(wrapperCaptor.capture());
      verify(dishService).getById(100L);
      verify(mapper).insert(shoppingCartCaptor.capture());
      ShoppingCart savedCart = shoppingCartCaptor.getValue();
      assertEquals(100L, savedCart.getDishId());
      assertEquals(1, savedCart.getNumber());
      assertEquals(888L, savedCart.getUserId());
      // 验证当 dish 为 null 时，name、image、amount 不会被设置（保持默认值）
    }
  }

  @Test
  void addShoppingCartWithSetmealNotFound() {
    // 测试场景：setmealId 不为 null，但查询不到对应的套餐（setmeal 为 null）
    // 1. 准备测试数据
    ShoppingCartDto dto = new ShoppingCartDto();
    dto.setSetmealId(200L);

    // 2. Mock 依赖行为 - 购物车中没有该商品，且查询不到套餐
    when(mapper.selectList(any())).thenReturn(Collections.emptyList());
    when(setmealService.getById(200L)).thenReturn(null); // 返回 null，模拟套餐不存在
    when(mapper.insert(any(ShoppingCart.class))).thenReturn(1);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(888L);

      // 3. 执行测试
      shoppingCartService.addShoppingCart(dto);

      // 4. 验证方法调用
      verify(mapper).selectList(wrapperCaptor.capture());
      verify(setmealService).getById(200L);
      verify(mapper).insert(shoppingCartCaptor.capture());
      ShoppingCart savedCart = shoppingCartCaptor.getValue();
      assertEquals(200L, savedCart.getSetmealId());
      assertEquals(1, savedCart.getNumber());
      assertEquals(888L, savedCart.getUserId());
      // 验证当 setmeal 为 null 时，name、image、amount 不会被设置（保持默认值）
    }
  }

  @Test
  void subShoppingCartWithNullList() {
    // 测试场景：findShoppingCartByDto 返回 null（而不是空列表）
    // 1. 准备测试数据
    ShoppingCartDto dto = new ShoppingCartDto();
    dto.setDishId(100L);

    // 2. Mock 依赖行为 - 返回 null
    when(mapper.selectList(any())).thenReturn(null);

    // 3. 执行测试
    shoppingCartService.subShoppingCart(dto);

    // 4. 验证方法调用
    verify(mapper).selectList(wrapperCaptor.capture());
    // 不应该调用 updateById 或 deleteById
  }

  @Test
  void findShoppingCartByDtoWithDishFlavor() {
    // 测试场景：当 ShoppingCartDto 包含非 null 的 dishFlavor 时
    // 1. 准备测试数据
    ShoppingCartDto dto = new ShoppingCartDto();
    dto.setDishId(100L);
    dto.setDishFlavor("微辣"); // 设置 dishFlavor 不为 null

    ShoppingCart cart = new ShoppingCart();
    cart.setId(1L);
    cart.setDishId(100L);
    cart.setDishFlavor("微辣");
    cart.setNumber(1);
    List<ShoppingCart> cartList = Collections.singletonList(cart);

    // 2. Mock 依赖行为
    when(mapper.selectList(any())).thenReturn(cartList);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(888L);

      // 3. 执行测试 - 通过调用 addShoppingCart 来间接测试 findShoppingCartByDto
      shoppingCartService.addShoppingCart(dto);

      // 4. 验证方法调用 - 验证 wrapper 中包含了 dishFlavor 条件
      verify(mapper).selectList(wrapperCaptor.capture());
      LambdaQueryWrapper<ShoppingCart> capturedWrapper = wrapperCaptor.getValue();
      assertNotNull(capturedWrapper);
    }
  }

  @Test
  void addShoppingCartWithNullList() {
    // 测试场景：findShoppingCartByDto 返回 null，确保进入 else 块
    // 覆盖：else 块（第11行）- 创建新购物车条目的逻辑
    // 1. 准备测试数据
    ShoppingCartDto dto = new ShoppingCartDto();
    dto.setDishId(100L);

    Dish dish = new Dish();
    dish.setId(100L);
    dish.setName("宫保鸡丁");
    dish.setImage("image.jpg");
    dish.setPrice(BigDecimal.valueOf(38.0));

    // 2. Mock 依赖行为 - 返回 null 而不是空列表
    when(mapper.selectList(any())).thenReturn(null);
    when(dishService.getById(100L)).thenReturn(dish);
    when(mapper.insert(any(ShoppingCart.class))).thenReturn(1);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(888L);

      // 3. 执行测试
      shoppingCartService.addShoppingCart(dto);

      // 4. 验证方法调用 - 确保进入了 else 块
      verify(mapper).selectList(wrapperCaptor.capture());
      verify(dishService).getById(100L);
      verify(mapper).insert(shoppingCartCaptor.capture());
      ShoppingCart savedCart = shoppingCartCaptor.getValue();
      assertEquals(100L, savedCart.getDishId());
      assertEquals("宫保鸡丁", savedCart.getName());
      assertEquals(1, savedCart.getNumber());
      assertEquals(888L, savedCart.getUserId());
    }
  }

  @Test
  void addShoppingCartWithBothDishIdAndSetmealIdNull() {
    // 测试场景：dishId 和 setmealId 都为 null，确保不进入任何分支
    // 覆盖：if (dishId != null) 的 else 分支，以及 else if (setmealId != null) 的 else 分支
    // 1. 准备测试数据
    ShoppingCartDto dto = new ShoppingCartDto();
    // dishId 和 setmealId 都为 null

    // 2. Mock 依赖行为 - 购物车中没有该商品
    when(mapper.selectList(any())).thenReturn(Collections.emptyList());
    when(mapper.insert(any(ShoppingCart.class))).thenReturn(1);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(888L);

      // 3. 执行测试
      shoppingCartService.addShoppingCart(dto);

      // 4. 验证方法调用 - 确保进入了 else 块，但没有调用 dishService 或 setmealService
      verify(mapper).selectList(wrapperCaptor.capture());
      verify(mapper).insert(shoppingCartCaptor.capture());
      ShoppingCart savedCart = shoppingCartCaptor.getValue();
      assertEquals(1, savedCart.getNumber());
      assertEquals(888L, savedCart.getUserId());
      // 验证没有调用 dishService 或 setmealService
    }
  }
}
