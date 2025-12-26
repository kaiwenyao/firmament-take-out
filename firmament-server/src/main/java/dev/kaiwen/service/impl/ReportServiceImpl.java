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
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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

    @Override
    public byte[] exportBusinessData() {
        // 计算最近30天的日期范围
        LocalDate end = LocalDate.now();
        LocalDate begin = end.minusDays(29);

        // 查询最近30天的所有订单
        List<Orders> ordersList = orderService.lambdaQuery()
                .ge(Orders::getOrderTime, begin.atStartOfDay())
                .le(Orders::getOrderTime, end.atTime(LocalTime.MAX))
                .list();

        // 查询最近30天的新增用户
        List<User> userList = userService.lambdaQuery()
                .ge(User::getCreateTime, begin.atStartOfDay())
                .le(User::getCreateTime, end.atTime(LocalTime.MAX))
                .list();

        // 按日期分组统计数据
        Map<LocalDate, DailyData> dailyDataMap = new HashMap<>();
        
        // 初始化所有日期
        LocalDate currentDate = begin;
        while (!currentDate.isAfter(end)) {
            dailyDataMap.put(currentDate, new DailyData(currentDate));
            currentDate = currentDate.plusDays(1);
        }

        // 统计订单数据
        for (Orders order : ordersList) {
            LocalDate orderDate = order.getOrderTime().toLocalDate();
            DailyData data = dailyDataMap.get(orderDate);
            if (data != null) {
                data.totalOrders++;
                if (order.getAmount() != null) {
                    data.turnover = data.turnover.add(order.getAmount());
                }
                if (Orders.COMPLETED.equals(order.getStatus())) {
                    data.validOrders++;
                }
            }
        }

        // 统计用户数据
        for (User user : userList) {
            LocalDate createDate = user.getCreateTime().toLocalDate();
            DailyData data = dailyDataMap.get(createDate);
            if (data != null) {
                data.newUsers++;
            }
        }

        // 计算概览数据（汇总）
        BigDecimal totalTurnover = BigDecimal.ZERO;
        int totalValidOrders = 0;
        int totalOrders = 0;
        int totalNewUsers = 0;
        
        for (DailyData data : dailyDataMap.values()) {
            totalTurnover = totalTurnover.add(data.turnover);
            totalValidOrders += data.validOrders;
            totalOrders += data.totalOrders;
            totalNewUsers += data.newUsers;
        }

        double orderCompletionRate = totalOrders > 0 ? (double) totalValidOrders / totalOrders : 0.0;
        double unitPrice = totalValidOrders > 0 ? totalTurnover.divide(BigDecimal.valueOf(totalValidOrders), 2, RoundingMode.HALF_UP).doubleValue() : 0.0;

        // 读取模板文件
        ClassPathResource templateResource = new ClassPathResource("template/model.xlsx");
        
        try (InputStream templateInputStream = templateResource.getInputStream();
             XSSFWorkbook workbook = new XSSFWorkbook(templateInputStream);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.getSheetAt(0); // 获取第一个工作表
            
            // 填充概览数据
            // 所有数据向右移动一个单元格（列索引+1），向下移动一个单元格（行索引+1）
            
            // 概览数据第一行（原第2行，现在第3行，索引为2）
            Row overviewRow1 = sheet.getRow(3);
            if (overviewRow1 == null) {
                overviewRow1 = sheet.createRow(3);
            }
            setCellValue(overviewRow1, 2, String.format("%.2f", totalTurnover.doubleValue())); // 营业额（原列1，现在列2）
            setCellValue(overviewRow1, 4, String.format("%.2f%%", orderCompletionRate * 100)); // 订单完成率（原列3，现在列4）
            setCellValue(overviewRow1, 6, String.valueOf(totalNewUsers)); // 新增用户数（原列5，现在列6）
            
            // 概览数据第二行（原第3行，现在第4行，索引为3）
            Row overviewRow2 = sheet.getRow(4);
            if (overviewRow2 == null) {
                overviewRow2 = sheet.createRow(4);
            }
            setCellValue(overviewRow2, 2, String.valueOf(totalValidOrders)); // 有效订单（原列1，现在列2）
            setCellValue(overviewRow2, 4, String.format("%.2f", unitPrice)); // 平均客单价（原列3，现在列4）

            // 填充明细数据
            // 明细数据从第7行开始（原第6行，现在第7行，索引为6）
            int detailStartRow = 7; // 明细数据开始行（向右向下各移动一个单元格）
            int currentRow = detailStartRow;
            
            currentDate = begin;
            while (!currentDate.isAfter(end)) {
                DailyData data = dailyDataMap.get(currentDate);
                Row dataRow = sheet.getRow(currentRow);
                if (dataRow == null) {
                    dataRow = sheet.createRow(currentRow);
                }
                
                double dailyOrderCompletionRate = data.totalOrders > 0 ? (double) data.validOrders / data.totalOrders : 0.0;
                double dailyUnitPrice = data.validOrders > 0 ? data.turnover.divide(BigDecimal.valueOf(data.validOrders), 2, RoundingMode.HALF_UP).doubleValue() : 0.0;

                // 填充明细数据（所有列索引+1）
                setCellValue(dataRow, 1, data.date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))); // 日期（原列0，现在列1）
                setCellValue(dataRow, 2, String.format("%.2f", data.turnover.doubleValue())); // 营业额（原列1，现在列2）
                setCellValue(dataRow, 3, String.valueOf(data.validOrders)); // 有效订单（原列2，现在列3）
                setCellValue(dataRow, 4, String.format("%.2f%%", dailyOrderCompletionRate * 100)); // 订单完成率（原列3，现在列4）
                setCellValue(dataRow, 5, String.format("%.2f", dailyUnitPrice)); // 平均客单价（原列4，现在列5）
                setCellValue(dataRow, 6, String.valueOf(data.newUsers)); // 新增用户数（原列5，现在列6）

                currentRow++;
                currentDate = currentDate.plusDays(1);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("导出Excel失败", e);
            throw new RuntimeException("导出Excel失败", e);
        }
    }

    /**
     * 设置单元格值（如果单元格不存在则创建）
     */
    private void setCellValue(Row row, int column, String value) {
        Cell cell = row.getCell(column);
        if (cell == null) {
            cell = row.createCell(column);
        }
        cell.setCellValue(value);
    }

    /**
     * 每日数据内部类
     */
    private static class DailyData {
        LocalDate date;
        BigDecimal turnover = BigDecimal.ZERO;
        int validOrders = 0;
        int totalOrders = 0;
        int newUsers = 0;

        DailyData(LocalDate date) {
            this.date = date;
        }
    }
}
