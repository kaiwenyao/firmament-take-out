package dev.kaiwen.service.impl;

import dev.kaiwen.entity.Orders;
import dev.kaiwen.entity.User;
import dev.kaiwen.service.OrderService;
import dev.kaiwen.service.ReportService;
import dev.kaiwen.service.UserService;
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
}
