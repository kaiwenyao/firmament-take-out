package dev.kaiwen.service.impl;

import dev.kaiwen.entity.OrderDetail;
import dev.kaiwen.entity.Orders;
import dev.kaiwen.entity.User;
import dev.kaiwen.service.OrderDetailService;
import dev.kaiwen.service.OrderService;
import dev.kaiwen.service.ReportService;
import dev.kaiwen.service.UserService;
import dev.kaiwen.vo.OrderReportVO;
import dev.kaiwen.vo.SalesTop10ReportVO;
import dev.kaiwen.vo.TurnoverReportVO;
import dev.kaiwen.vo.UserReportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final OrderService orderService;
    private final UserService userService;
    private final OrderDetailService orderDetailService;

    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        // 查询指定日期范围内的已完成订单
        List<Orders> ordersList = orderService.lambdaQuery()
                .eq(Orders::getStatus, Orders.COMPLETED)
                .ge(Orders::getOrderTime, begin.atStartOfDay())
                .le(Orders::getOrderTime, end.atTime(LocalTime.MAX))
                .list();

        // 按日期分组统计营业额
        Map<LocalDate, BigDecimal> turnoverMap = new HashMap<>();
        for (Orders order : ordersList) {
            LocalDate orderDate = order.getOrderTime().toLocalDate();
            BigDecimal amount = order.getAmount() != null ? order.getAmount() : BigDecimal.ZERO;
            turnoverMap.put(orderDate, turnoverMap.getOrDefault(orderDate, BigDecimal.ZERO).add(amount));
        }

        // 填充日期范围内的所有日期
        List<LocalDate> dateList = new ArrayList<>();
        List<BigDecimal> turnoverList = new ArrayList<>();
        
        LocalDate currentDate = begin;
        while (!currentDate.isAfter(end)) {
            dateList.add(currentDate);
            turnoverList.add(turnoverMap.getOrDefault(currentDate, BigDecimal.ZERO));
            currentDate = currentDate.plusDays(1);
        }

        // 构建返回对象
        String dateListStr = dateList.stream()
                .map(LocalDate::toString)
                .collect(Collectors.joining(","));
        
        String turnoverListStr = turnoverList.stream()
                .map(BigDecimal::toString)
                .collect(Collectors.joining(","));

        return TurnoverReportVO.builder()
                .dateList(dateListStr)
                .turnoverList(turnoverListStr)
                .build();
    }

    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        // 查询指定日期范围内注册的用户
        List<User> userList = userService.lambdaQuery()
                .ge(User::getCreateTime, begin.atStartOfDay())
                .le(User::getCreateTime, end.atTime(LocalTime.MAX))
                .list();

        // 按日期分组统计每天新增的用户数
        Map<LocalDate, Integer> newUserMap = new HashMap<>();
        for (User user : userList) {
            LocalDate createDate = user.getCreateTime().toLocalDate();
            newUserMap.put(createDate, newUserMap.getOrDefault(createDate, 0) + 1);
        }

        // 填充日期范围内的所有日期，并计算累计用户总量
        List<LocalDate> dateList = new ArrayList<>();
        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();
        
        // 查询开始日期之前的总用户数（作为基准）
        long baseUserCount = userService.lambdaQuery()
                .lt(User::getCreateTime, begin.atStartOfDay())
                .count();
        
        LocalDate currentDate = begin;
        int cumulativeCount = (int) baseUserCount;
        
        while (!currentDate.isAfter(end)) {
            dateList.add(currentDate);
            
            // 当天新增用户数
            int newUserCount = newUserMap.getOrDefault(currentDate, 0);
            newUserList.add(newUserCount);
            
            // 累计用户总量（基准数 + 从开始日期到当前日期的累计新增）
            cumulativeCount += newUserCount;
            totalUserList.add(cumulativeCount);
            
            currentDate = currentDate.plusDays(1);
        }

        // 构建返回对象
        String dateListStr = dateList.stream()
                .map(LocalDate::toString)
                .collect(Collectors.joining(","));
        
        String newUserListStr = newUserList.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        
        String totalUserListStr = totalUserList.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        return UserReportVO.builder()
                .dateList(dateListStr)
                .newUserList(newUserListStr)
                .totalUserList(totalUserListStr)
                .build();
    }

    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        // 查询指定日期范围内的所有订单
        List<Orders> ordersList = orderService.lambdaQuery()
                .ge(Orders::getOrderTime, begin.atStartOfDay())
                .le(Orders::getOrderTime, end.atTime(LocalTime.MAX))
                .list();

        // 按日期分组统计每天的订单总数和有效订单数
        Map<LocalDate, Integer> orderCountMap = new HashMap<>();
        Map<LocalDate, Integer> validOrderCountMap = new HashMap<>();
        
        int totalOrderCount = 0;
        int validOrderCount = 0;
        
        for (Orders order : ordersList) {
            LocalDate orderDate = order.getOrderTime().toLocalDate();
            
            // 统计每日订单总数
            orderCountMap.put(orderDate, orderCountMap.getOrDefault(orderDate, 0) + 1);
            totalOrderCount++;
            
            // 统计每日有效订单数（已完成订单）
            if (Orders.COMPLETED.equals(order.getStatus())) {
                validOrderCountMap.put(orderDate, validOrderCountMap.getOrDefault(orderDate, 0) + 1);
                validOrderCount++;
            }
        }

        // 填充日期范围内的所有日期
        List<LocalDate> dateList = new ArrayList<>();
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();
        
        LocalDate currentDate = begin;
        while (!currentDate.isAfter(end)) {
            dateList.add(currentDate);
            orderCountList.add(orderCountMap.getOrDefault(currentDate, 0));
            validOrderCountList.add(validOrderCountMap.getOrDefault(currentDate, 0));
            currentDate = currentDate.plusDays(1);
        }

        // 计算订单完成率
        Double orderCompletionRate = totalOrderCount > 0 
                ? (double) validOrderCount / totalOrderCount 
                : 0.0;

        // 构建返回对象
        String dateListStr = dateList.stream()
                .map(LocalDate::toString)
                .collect(Collectors.joining(","));
        
        String orderCountListStr = orderCountList.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        
        String validOrderCountListStr = validOrderCountList.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        return OrderReportVO.builder()
                .dateList(dateListStr)
                .orderCountList(orderCountListStr)
                .validOrderCountList(validOrderCountListStr)
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        // 查询指定日期范围内的已完成订单
        List<Orders> ordersList = orderService.lambdaQuery()
                .eq(Orders::getStatus, Orders.COMPLETED)
                .ge(Orders::getOrderTime, begin.atStartOfDay())
                .le(Orders::getOrderTime, end.atTime(LocalTime.MAX))
                .list();

        // 获取订单ID列表
        List<Long> orderIds = ordersList.stream()
                .map(Orders::getId)
                .collect(Collectors.toList());

        // 如果没有订单，返回空数据
        if (orderIds.isEmpty()) {
            return SalesTop10ReportVO.builder()
                    .nameList("")
                    .numberList("")
                    .build();
        }

        // 查询这些订单的订单明细
        List<OrderDetail> orderDetailList = orderDetailService.lambdaQuery()
                .in(OrderDetail::getOrderId, orderIds)
                .list();

        // 按商品名称分组统计总销量
        Map<String, Integer> salesMap = new HashMap<>();
        for (OrderDetail orderDetail : orderDetailList) {
            String name = orderDetail.getName();
            Integer number = orderDetail.getNumber() != null ? orderDetail.getNumber() : 0;
            salesMap.put(name, salesMap.getOrDefault(name, 0) + number);
        }

        // 按销量降序排序，取前10个
        List<Map.Entry<String, Integer>> top10List = salesMap.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(10)
                .collect(Collectors.toList());

        // 构建返回对象
        List<String> nameList = new ArrayList<>();
        List<Integer> numberList = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : top10List) {
            nameList.add(entry.getKey());
            numberList.add(entry.getValue());
        }

        String nameListStr = String.join(",", nameList);
        String numberListStr = numberList.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        return SalesTop10ReportVO.builder()
                .nameList(nameListStr)
                .numberList(numberListStr)
                .build();
    }
}
