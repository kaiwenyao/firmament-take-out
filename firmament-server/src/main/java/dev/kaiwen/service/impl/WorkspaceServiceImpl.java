package dev.kaiwen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import dev.kaiwen.constant.StatusConstant;
import dev.kaiwen.entity.Dish;
import dev.kaiwen.entity.Orders;
import dev.kaiwen.entity.Setmeal;
import dev.kaiwen.entity.User;
import dev.kaiwen.mapper.DishMapper;
import dev.kaiwen.mapper.OrderMapper;
import dev.kaiwen.mapper.SetmealMapper;
import dev.kaiwen.mapper.UserMapper;
import dev.kaiwen.service.DishService;
import dev.kaiwen.service.OrderService;
import dev.kaiwen.service.SetmealService;
import dev.kaiwen.service.UserService;
import dev.kaiwen.service.WorkspaceService;
import dev.kaiwen.vo.BusinessDataVo;
import dev.kaiwen.vo.DishOverViewVo;
import dev.kaiwen.vo.OrderOverViewVo;
import dev.kaiwen.vo.SetmealOverViewVo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 工作台服务实现类.
 * 提供营业数据统计、订单概览、菜品概览、套餐概览等功能.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WorkspaceServiceImpl implements WorkspaceService {

  private final OrderMapper orderMapper;
  private final UserMapper userMapper;
  private final DishMapper dishMapper;
  private final SetmealMapper setmealMapper;

  /**
   * 根据时间段统计营业数据.
   *
   * @param begin 开始时间
   * @param end   结束时间
   * @return 营业数据
   */
  @Override
  public BusinessDataVo getBusinessData(LocalDateTime begin, LocalDateTime end) {
    // 营业额：当日已完成订单的总金额
    // 有效订单：当日已完成订单的数量
    // 订单完成率：有效订单数 / 总订单数
    // 平均客单价：营业额 / 有效订单数
    // 新增用户：当日新增用户的数量

    // 查询总订单数
    // 使用 Wrappers + mapper 方式查询
    LambdaQueryWrapper<Orders> totalOrderWrapper = Wrappers.lambdaQuery(Orders.class)
        .ge(Orders::getOrderTime, begin)
        .le(Orders::getOrderTime, end);
    long totalOrderCount = orderMapper.selectCount(totalOrderWrapper);

    // 查询已完成订单（有效订单）
    // 使用 Wrappers + mapper 方式查询
    LambdaQueryWrapper<Orders> completedOrderWrapper = Wrappers.lambdaQuery(Orders.class)
        .eq(Orders::getStatus, Orders.COMPLETED)
        .ge(Orders::getOrderTime, begin)
        .le(Orders::getOrderTime, end);
    List<Orders> completedOrders = orderMapper.selectList(completedOrderWrapper);

    // 计算营业额和有效订单数
    BigDecimal turnover = BigDecimal.ZERO;
    int validOrderCount = completedOrders.size();

    for (Orders order : completedOrders) {
      if (order.getAmount() != null) {
        turnover = turnover.add(order.getAmount());
      }
    }

    // 计算订单完成率和平均客单价
    double orderCompletionRate = 0.0;
    double unitPrice = 0.0;

    if (totalOrderCount > 0 && validOrderCount > 0) {
      orderCompletionRate = (double) validOrderCount / totalOrderCount;
      unitPrice = turnover.divide(BigDecimal.valueOf(validOrderCount), 2,
              java.math.RoundingMode.HALF_UP)
          .doubleValue();
    }

    // 查询新增用户数
    // 使用 Wrappers + mapper 方式查询
    LambdaQueryWrapper<User> userWrapper = Wrappers.lambdaQuery(User.class)
        .ge(User::getCreateTime, begin)
        .le(User::getCreateTime, end);
    long newUsers = userMapper.selectCount(userWrapper);

    return BusinessDataVo.builder()
        .turnover(turnover.doubleValue())
        .validOrderCount(validOrderCount)
        .orderCompletionRate(orderCompletionRate)
        .unitPrice(unitPrice)
        .newUsers((int) newUsers)
        .build();
  }

  /**
   * 查询订单管理数据.
   *
   * @return 订单概览数据
   */
  @Override
  public OrderOverViewVo getOrderOverView() {
    LocalDateTime begin = LocalDateTime.now().with(LocalTime.MIN);

    // 待接单
    // 使用 Wrappers + mapper 方式查询
    LambdaQueryWrapper<Orders> waitingWrapper = Wrappers.lambdaQuery(Orders.class)
        .ge(Orders::getOrderTime, begin)
        .eq(Orders::getStatus, Orders.TO_BE_CONFIRMED);
    long waitingOrders = orderMapper.selectCount(waitingWrapper);

    // 待派送
    LambdaQueryWrapper<Orders> deliveredWrapper = Wrappers.lambdaQuery(Orders.class)
        .ge(Orders::getOrderTime, begin)
        .eq(Orders::getStatus, Orders.CONFIRMED);
    long deliveredOrders = orderMapper.selectCount(deliveredWrapper);

    // 已完成
    LambdaQueryWrapper<Orders> completedWrapper = Wrappers.lambdaQuery(Orders.class)
        .ge(Orders::getOrderTime, begin)
        .eq(Orders::getStatus, Orders.COMPLETED);
    long completedOrders = orderMapper.selectCount(completedWrapper);

    // 已取消
    LambdaQueryWrapper<Orders> cancelledWrapper = Wrappers.lambdaQuery(Orders.class)
        .ge(Orders::getOrderTime, begin)
        .eq(Orders::getStatus, Orders.CANCELLED);
    long cancelledOrders = orderMapper.selectCount(cancelledWrapper);

    // 全部订单
    LambdaQueryWrapper<Orders> allWrapper = Wrappers.lambdaQuery(Orders.class)
        .ge(Orders::getOrderTime, begin);
    long allOrders = orderMapper.selectCount(allWrapper);

    return OrderOverViewVo.builder()
        .waitingOrders((int) waitingOrders)
        .deliveredOrders((int) deliveredOrders)
        .completedOrders((int) completedOrders)
        .cancelledOrders((int) cancelledOrders)
        .allOrders((int) allOrders)
        .build();
  }

  /**
   * 查询菜品总览.
   *
   * @return 菜品概览数据
   */
  @Override
  public DishOverViewVo getDishOverView() {
    // 已启售数量
    // 使用 Wrappers + mapper 方式查询
    LambdaQueryWrapper<Dish> soldWrapper = Wrappers.lambdaQuery(Dish.class)
        .eq(Dish::getStatus, StatusConstant.ENABLE);
    long sold = dishMapper.selectCount(soldWrapper);

    // 已停售数量
    LambdaQueryWrapper<Dish> discontinuedWrapper = Wrappers.lambdaQuery(Dish.class)
        .eq(Dish::getStatus, StatusConstant.DISABLE);
    long discontinued = dishMapper.selectCount(discontinuedWrapper);

    return DishOverViewVo.builder()
        .sold((int) sold)
        .discontinued((int) discontinued)
        .build();
  }

  /**
   * 查询套餐总览.
   *
   * @return 套餐概览数据
   */
  @Override
  public SetmealOverViewVo getSetmealOverView() {
    // 已启售数量
    // 使用 Wrappers + mapper 方式查询
    LambdaQueryWrapper<Setmeal> soldWrapper = Wrappers.lambdaQuery(Setmeal.class)
        .eq(Setmeal::getStatus, StatusConstant.ENABLE);
    long sold = setmealMapper.selectCount(soldWrapper);

    // 已停售数量
    LambdaQueryWrapper<Setmeal> discontinuedWrapper = Wrappers.lambdaQuery(Setmeal.class)
        .eq(Setmeal::getStatus, StatusConstant.DISABLE);
    long discontinued = setmealMapper.selectCount(discontinuedWrapper);

    return SetmealOverViewVo.builder()
        .sold((int) sold)
        .discontinued((int) discontinued)
        .build();
  }
}

