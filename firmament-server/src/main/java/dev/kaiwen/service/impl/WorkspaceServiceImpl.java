package dev.kaiwen.service.impl;

import dev.kaiwen.constant.StatusConstant;
import dev.kaiwen.entity.Dish;
import dev.kaiwen.entity.Orders;
import dev.kaiwen.entity.Setmeal;
import dev.kaiwen.service.DishService;
import dev.kaiwen.service.OrderService;
import dev.kaiwen.service.SetmealService;
import dev.kaiwen.service.UserService;
import dev.kaiwen.service.WorkspaceService;
import dev.kaiwen.vo.BusinessDataVo;
import dev.kaiwen.vo.DishOverViewVo;
import dev.kaiwen.vo.OrderOverViewVo;
import dev.kaiwen.vo.SetmealOverViewVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkspaceServiceImpl implements WorkspaceService {

    private final OrderService orderService;
    private final UserService userService;
    private final DishService dishService;
    private final SetmealService setmealService;

    /**
     * 根据时间段统计营业数据
     * @param begin 开始时间
     * @param end 结束时间
     * @return 营业数据
     */
    @Override
    public BusinessDataVo getBusinessData(LocalDateTime begin, LocalDateTime end) {
        /**
         * 营业额：当日已完成订单的总金额
         * 有效订单：当日已完成订单的数量
         * 订单完成率：有效订单数 / 总订单数
         * 平均客单价：营业额 / 有效订单数
         * 新增用户：当日新增用户的数量
         */

        // 查询总订单数
        long totalOrderCount = orderService.lambdaQuery()
                .ge(Orders::getOrderTime, begin)
                .le(Orders::getOrderTime, end)
                .count();

        // 查询已完成订单（有效订单）
        List<Orders> completedOrders = orderService.lambdaQuery()
                .eq(Orders::getStatus, Orders.COMPLETED)
                .ge(Orders::getOrderTime, begin)
                .le(Orders::getOrderTime, end)
                .list();

        // 计算营业额和有效订单数
        BigDecimal turnover = BigDecimal.ZERO;
        int validOrderCount = completedOrders.size();
        
        for (Orders order : completedOrders) {
            if (order.getAmount() != null) {
                turnover = turnover.add(order.getAmount());
            }
        }

        // 计算订单完成率和平均客单价
        Double orderCompletionRate = 0.0;
        Double unitPrice = 0.0;
        
        if (totalOrderCount > 0 && validOrderCount > 0) {
            orderCompletionRate = (double) validOrderCount / totalOrderCount;
            unitPrice = turnover.divide(BigDecimal.valueOf(validOrderCount), 2, java.math.RoundingMode.HALF_UP)
                    .doubleValue();
        }

        // 查询新增用户数
        long newUsers = userService.lambdaQuery()
                .ge(dev.kaiwen.entity.User::getCreateTime, begin)
                .le(dev.kaiwen.entity.User::getCreateTime, end)
                .count();

        return BusinessDataVo.builder()
                .turnover(turnover.doubleValue())
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .newUsers((int) newUsers)
                .build();
    }

    /**
     * 查询订单管理数据
     * @return 订单概览数据
     */
    @Override
    public OrderOverViewVo getOrderOverView() {
        LocalDateTime begin = LocalDateTime.now().with(LocalTime.MIN);

        // 待接单
        long waitingOrders = orderService.lambdaQuery()
                .ge(Orders::getOrderTime, begin)
                .eq(Orders::getStatus, Orders.TO_BE_CONFIRMED)
                .count();

        // 待派送
        long deliveredOrders = orderService.lambdaQuery()
                .ge(Orders::getOrderTime, begin)
                .eq(Orders::getStatus, Orders.CONFIRMED)
                .count();

        // 已完成
        long completedOrders = orderService.lambdaQuery()
                .ge(Orders::getOrderTime, begin)
                .eq(Orders::getStatus, Orders.COMPLETED)
                .count();

        // 已取消
        long cancelledOrders = orderService.lambdaQuery()
                .ge(Orders::getOrderTime, begin)
                .eq(Orders::getStatus, Orders.CANCELLED)
                .count();

        // 全部订单
        long allOrders = orderService.lambdaQuery()
                .ge(Orders::getOrderTime, begin)
                .count();

        return OrderOverViewVo.builder()
                .waitingOrders((int) waitingOrders)
                .deliveredOrders((int) deliveredOrders)
                .completedOrders((int) completedOrders)
                .cancelledOrders((int) cancelledOrders)
                .allOrders((int) allOrders)
                .build();
    }

    /**
     * 查询菜品总览
     * @return 菜品概览数据
     */
    @Override
    public DishOverViewVo getDishOverView() {
        // 已启售数量
        long sold = dishService.lambdaQuery()
                .eq(Dish::getStatus, StatusConstant.ENABLE)
                .count();

        // 已停售数量
        long discontinued = dishService.lambdaQuery()
                .eq(Dish::getStatus, StatusConstant.DISABLE)
                .count();

        return DishOverViewVo.builder()
                .sold((int) sold)
                .discontinued((int) discontinued)
                .build();
    }

    /**
     * 查询套餐总览
     * @return 套餐概览数据
     */
    @Override
    public SetmealOverViewVo getSetmealOverView() {
        // 已启售数量
        long sold = setmealService.lambdaQuery()
                .eq(Setmeal::getStatus, StatusConstant.ENABLE)
                .count();

        // 已停售数量
        long discontinued = setmealService.lambdaQuery()
                .eq(Setmeal::getStatus, StatusConstant.DISABLE)
                .count();

        return SetmealOverViewVo.builder()
                .sold((int) sold)
                .discontinued((int) discontinued)
                .build();
    }
}

