package dev.kaiwen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.kaiwen.entity.Orders;
import dev.kaiwen.service.IOrderService;
import dev.kaiwen.service.IReportService;
import dev.kaiwen.vo.TurnoverReportVO;
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
public class ReportServiceImpl implements IReportService {

    private final IOrderService orderService;

    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        // 查询指定日期范围内的已完成订单
        List<Orders> ordersList = orderService.list(
                new LambdaQueryWrapper<Orders>()
                        .eq(Orders::getStatus, Orders.COMPLETED)
                        .ge(Orders::getOrderTime, begin.atStartOfDay())
                        .le(Orders::getOrderTime, end.atTime(LocalTime.MAX))
        );

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
}
