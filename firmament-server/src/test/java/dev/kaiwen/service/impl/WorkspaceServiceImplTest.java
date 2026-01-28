package dev.kaiwen.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import dev.kaiwen.entity.Dish;
import dev.kaiwen.entity.Orders;
import dev.kaiwen.entity.Setmeal;
import dev.kaiwen.entity.User;
import dev.kaiwen.mapper.DishMapper;
import dev.kaiwen.mapper.OrderMapper;
import dev.kaiwen.mapper.SetmealMapper;
import dev.kaiwen.mapper.UserMapper;
import dev.kaiwen.vo.BusinessDataVo;
import dev.kaiwen.vo.DishOverViewVo;
import dev.kaiwen.vo.OrderOverViewVo;
import dev.kaiwen.vo.SetmealOverViewVo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceImplTest {

  @InjectMocks
  private WorkspaceServiceImpl workspaceService;

  @Mock
  private OrderMapper orderMapper;

  @Mock
  private UserMapper userMapper;

  @Mock
  private DishMapper dishMapper;

  @Mock
  private SetmealMapper setmealMapper;

  @Captor
  private ArgumentCaptor<LambdaQueryWrapper<Orders>> ordersWrapperCaptor;

  @Captor
  private ArgumentCaptor<LambdaQueryWrapper<User>> userWrapperCaptor;

  @BeforeEach
  void setUp() {
    MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
    TableInfoHelper.initTableInfo(assistant, Orders.class);
    TableInfoHelper.initTableInfo(assistant, User.class);
    TableInfoHelper.initTableInfo(assistant, Dish.class);
    TableInfoHelper.initTableInfo(assistant, Setmeal.class);
  }

  @Test
  void getBusinessDataSuccess() {
    Orders completedOrder1 = new Orders();
    completedOrder1.setAmount(BigDecimal.valueOf(50.0));
    completedOrder1.setStatus(Orders.COMPLETED);
    Orders completedOrder2 = new Orders();
    completedOrder2.setAmount(BigDecimal.valueOf(30.0));
    completedOrder2.setStatus(Orders.COMPLETED);
    List<Orders> completedOrders = List.of(completedOrder1, completedOrder2);

    // 2. Mock 依赖行为
    when(orderMapper.selectCount(any())).thenReturn(100L);
    when(orderMapper.selectList(any())).thenReturn(completedOrders);
    when(userMapper.selectCount(any())).thenReturn(10L);

    // 3. 执行测试
    LocalDateTime begin = LocalDateTime.now().minusDays(1);
    LocalDateTime end = LocalDateTime.now();
    BusinessDataVo result = workspaceService.getBusinessData(begin, end);

    // 4. 验证结果
    assertNotNull(result);
    assertEquals(2, result.getValidOrderCount());
    assertEquals(80.0, result.getTurnover());
    assertEquals(10, result.getNewUsers());
    assertEquals(0.02, result.getOrderCompletionRate());
    assertEquals(40.0, result.getUnitPrice());

    // 5. 验证方法调用
    verify(orderMapper).selectCount(ordersWrapperCaptor.capture());
    verify(orderMapper).selectList(ordersWrapperCaptor.capture());
    verify(userMapper).selectCount(userWrapperCaptor.capture());
  }

  @Test
  void getOrderOverViewSuccess() {
    // 1. Mock 依赖行为
    when(orderMapper.selectCount(any())).thenReturn(10L, 20L, 30L, 5L, 65L);

    // 2. 执行测试
    OrderOverViewVo result = workspaceService.getOrderOverView();

    // 3. 验证结果
    assertNotNull(result);
    assertEquals(10, result.getWaitingOrders());
    assertEquals(20, result.getDeliveredOrders());
    assertEquals(30, result.getCompletedOrders());
    assertEquals(5, result.getCancelledOrders());
    assertEquals(65, result.getAllOrders());

    // 4. 验证方法调用（应该调用5次 selectCount）
    verify(orderMapper, times(5)).selectCount(any());
  }

  @Test
  void getDishOverViewSuccess() {
    // 1. Mock 依赖行为
    when(dishMapper.selectCount(any())).thenReturn(50L, 20L);

    // 2. 执行测试
    DishOverViewVo result = workspaceService.getDishOverView();

    // 3. 验证结果
    assertNotNull(result);
    assertEquals(50, result.getSold());
    assertEquals(20, result.getDiscontinued());

    // 4. 验证方法调用（应该调用2次 selectCount）
    verify(dishMapper, times(2)).selectCount(any());
  }

  @Test
  void getSetmealOverViewSuccess() {
    // 1. Mock 依赖行为
    when(setmealMapper.selectCount(any())).thenReturn(30L, 10L);

    // 2. 执行测试
    SetmealOverViewVo result = workspaceService.getSetmealOverView();

    // 3. 验证结果
    assertNotNull(result);
    assertEquals(30, result.getSold());
    assertEquals(10, result.getDiscontinued());

    // 4. 验证方法调用（应该调用2次 selectCount）
    verify(setmealMapper, times(2)).selectCount(any());
  }

  @Test
  void getBusinessDataWithNoOrders() {
    // 2. Mock 依赖行为
    when(orderMapper.selectCount(any())).thenReturn(0L);
    when(orderMapper.selectList(any())).thenReturn(Collections.emptyList());
    when(userMapper.selectCount(any())).thenReturn(0L);

    // 3. 执行测试
    LocalDateTime begin = LocalDateTime.now().minusDays(1);
    LocalDateTime end = LocalDateTime.now();
    BusinessDataVo result = workspaceService.getBusinessData(begin, end);

    // 4. 验证结果
    assertNotNull(result);
    assertEquals(0, result.getValidOrderCount());
    assertEquals(0.0, result.getTurnover());
    assertEquals(0, result.getNewUsers());
    assertEquals(0.0, result.getOrderCompletionRate());
    assertEquals(0.0, result.getUnitPrice());

    // 5. 验证方法调用
    verify(orderMapper).selectCount(ordersWrapperCaptor.capture());
    verify(orderMapper).selectList(ordersWrapperCaptor.capture());
    verify(userMapper).selectCount(userWrapperCaptor.capture());
  }

  @Test
  void getBusinessDataWithNullAmount() {
    // 1. 准备测试数据 - 测试 order.getAmount() == null 的分支
    Orders completedOrder1 = new Orders();
    completedOrder1.setAmount(null); // 测试 null 分支
    completedOrder1.setStatus(Orders.COMPLETED);
    Orders completedOrder2 = new Orders();
    completedOrder2.setAmount(BigDecimal.valueOf(30.0));
    completedOrder2.setStatus(Orders.COMPLETED);
    List<Orders> completedOrders = List.of(completedOrder1, completedOrder2);

    // 2. Mock 依赖行为
    when(orderMapper.selectCount(any())).thenReturn(100L);
    when(orderMapper.selectList(any())).thenReturn(completedOrders);
    when(userMapper.selectCount(any())).thenReturn(10L);

    // 3. 执行测试
    LocalDateTime begin = LocalDateTime.now().minusDays(1);
    LocalDateTime end = LocalDateTime.now();
    BusinessDataVo result = workspaceService.getBusinessData(begin, end);

    // 4. 验证结果 - 只有 order2 的金额被计算
    assertNotNull(result);
    assertEquals(2, result.getValidOrderCount());
    assertEquals(30.0, result.getTurnover()); // 只有 order2 的金额
    assertEquals(10, result.getNewUsers());
    assertEquals(0.02, result.getOrderCompletionRate());
    assertEquals(15.0, result.getUnitPrice()); // 30.0 / 2

    // 5. 验证方法调用
    verify(orderMapper).selectCount(ordersWrapperCaptor.capture());
    verify(orderMapper).selectList(ordersWrapperCaptor.capture());
    verify(userMapper).selectCount(userWrapperCaptor.capture());
  }

  @Test
  void getBusinessDataWithZeroTotalOrderCount() {
    // 1. 准备测试数据 - 测试 totalOrderCount <= 0 的分支
    Orders completedOrder1 = new Orders();
    completedOrder1.setAmount(BigDecimal.valueOf(50.0));
    completedOrder1.setStatus(Orders.COMPLETED);
    List<Orders> completedOrders = List.of(completedOrder1);

    // 2. Mock 依赖行为 - totalOrderCount = 0
    when(orderMapper.selectCount(any())).thenReturn(0L);
    when(orderMapper.selectList(any())).thenReturn(completedOrders);
    when(userMapper.selectCount(any())).thenReturn(10L);

    // 3. 执行测试
    LocalDateTime begin = LocalDateTime.now().minusDays(1);
    LocalDateTime end = LocalDateTime.now();
    BusinessDataVo result = workspaceService.getBusinessData(begin, end);

    // 4. 验证结果 - 当 totalOrderCount = 0 时，orderCompletionRate 和 unitPrice 应该为 0.0
    assertNotNull(result);
    assertEquals(1, result.getValidOrderCount());
    assertEquals(50.0, result.getTurnover());
    assertEquals(10, result.getNewUsers());
    assertEquals(0.0, result.getOrderCompletionRate()); // totalOrderCount = 0，所以完成率为 0
    assertEquals(0.0, result.getUnitPrice()); // totalOrderCount = 0，所以单价为 0

    // 5. 验证方法调用
    verify(orderMapper).selectCount(ordersWrapperCaptor.capture());
    verify(orderMapper).selectList(ordersWrapperCaptor.capture());
    verify(userMapper).selectCount(userWrapperCaptor.capture());
  }

  @Test
  void getBusinessDataWithZeroValidOrderCount() {
    // 1. 准备测试数据 - 测试 validOrderCount <= 0 的分支
    // 2. Mock 依赖行为 - validOrderCount = 0 (completedOrders 为空)
    when(orderMapper.selectCount(any())).thenReturn(100L);
    when(orderMapper.selectList(any())).thenReturn(Collections.emptyList());
    when(userMapper.selectCount(any())).thenReturn(10L);

    // 3. 执行测试
    LocalDateTime begin = LocalDateTime.now().minusDays(1);
    LocalDateTime end = LocalDateTime.now();
    BusinessDataVo result = workspaceService.getBusinessData(begin, end);

    // 4. 验证结果 - 当 validOrderCount = 0 时，orderCompletionRate 和 unitPrice 应该为 0.0
    assertNotNull(result);
    assertEquals(0, result.getValidOrderCount());
    assertEquals(0.0, result.getTurnover());
    assertEquals(10, result.getNewUsers());
    assertEquals(0.0, result.getOrderCompletionRate()); // validOrderCount = 0，所以完成率为 0
    assertEquals(0.0, result.getUnitPrice()); // validOrderCount = 0，所以单价为 0

    // 5. 验证方法调用
    verify(orderMapper).selectCount(ordersWrapperCaptor.capture());
    verify(orderMapper).selectList(ordersWrapperCaptor.capture());
    verify(userMapper).selectCount(userWrapperCaptor.capture());
  }
}
