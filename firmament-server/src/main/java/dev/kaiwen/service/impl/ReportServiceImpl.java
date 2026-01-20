package dev.kaiwen.service.impl;

import dev.kaiwen.entity.OrderDetail;
import dev.kaiwen.entity.Orders;
import dev.kaiwen.entity.User;
import dev.kaiwen.service.OrderDetailService;
import dev.kaiwen.service.OrderService;
import dev.kaiwen.service.ReportService;
import dev.kaiwen.service.UserService;
import dev.kaiwen.vo.OrderReportVo;
import dev.kaiwen.vo.SalesTop10ReportVo;
import dev.kaiwen.vo.TurnoverReportVo;
import dev.kaiwen.vo.UserReportVo;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * 报表服务实现类.
 * 提供营业额统计、用户统计、订单统计、销量Top10和业务数据导出等功能.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

  private final OrderService orderService;
  private final UserService userService;
  private final OrderDetailService orderDetailService;

  @Override
  public TurnoverReportVo getTurnoverStatistics(LocalDate begin, LocalDate end) {
    // 验证日期参数
    validateDateRange(begin, end);

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
    String dateListStr = buildDateListString(dateList);
    String turnoverListStr = buildBigDecimalListString(turnoverList);

    return TurnoverReportVo.builder()
        .dateList(dateListStr)
        .turnoverList(turnoverListStr)
        .build();
  }

  @Override
  public UserReportVo getUserStatistics(LocalDate begin, LocalDate end) {
    // 验证日期参数
    validateDateRange(begin, end);

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
    String dateListStr = buildDateListString(dateList);
    String newUserListStr = buildIntegerListString(newUserList);
    String totalUserListStr = buildIntegerListString(totalUserList);

    return UserReportVo.builder()
        .dateList(dateListStr)
        .newUserList(newUserListStr)
        .totalUserList(totalUserListStr)
        .build();
  }

  @Override
  public OrderReportVo getOrderStatistics(LocalDate begin, LocalDate end) {
    // 验证日期参数
    validateDateRange(begin, end);

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
    String dateListStr = buildDateListString(dateList);
    String orderCountListStr = buildIntegerListString(orderCountList);
    String validOrderCountListStr = buildIntegerListString(validOrderCountList);

    return OrderReportVo.builder()
        .dateList(dateListStr)
        .orderCountList(orderCountListStr)
        .validOrderCountList(validOrderCountListStr)
        .totalOrderCount(totalOrderCount)
        .validOrderCount(validOrderCount)
        .orderCompletionRate(orderCompletionRate)
        .build();
  }

  @Override
  public SalesTop10ReportVo getSalesTop10(LocalDate begin, LocalDate end) {
    // 验证日期参数
    validateDateRange(begin, end);

    // 查询指定日期范围内的已完成订单
    List<Orders> ordersList = orderService.lambdaQuery()
        .eq(Orders::getStatus, Orders.COMPLETED)
        .ge(Orders::getOrderTime, begin.atStartOfDay())
        .le(Orders::getOrderTime, end.atTime(LocalTime.MAX))
        .list();

    // 获取订单ID列表
    List<Long> orderIds = ordersList.stream()
        .map(Orders::getId)
        .toList();

    // 如果没有订单，返回空数据
    if (orderIds.isEmpty()) {
      return SalesTop10ReportVo.builder()
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
        .toList();

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

    return SalesTop10ReportVo.builder()
        .nameList(nameListStr)
        .numberList(numberListStr)
        .build();
  }

  @Override
  public byte[] exportBusinessData() {
    // 计算最近30天的日期范围
    LocalDate end = LocalDate.now();
    LocalDate begin = end.minusDays(29);

    // 查询数据
    List<Orders> ordersList = queryOrdersInRange(begin, end);
    List<User> userList = queryUsersInRange(begin, end);

    // 按日期分组统计数据
    Map<LocalDate, DailyData> dailyDataMap = initializeDailyDataMap(begin, end);
    aggregateOrderData(ordersList, dailyDataMap);
    aggregateUserData(userList, dailyDataMap);

    // 计算概览数据
    OverviewStatistics overview = calculateOverviewStatistics(dailyDataMap);

    // 生成Excel
    return generateExcel(dailyDataMap, overview, begin, end);
  }

  /**
   * 查询指定日期范围内的订单.
   *
   * @param begin 开始日期
   * @param end   结束日期
   * @return 订单列表
   */
  private List<Orders> queryOrdersInRange(LocalDate begin, LocalDate end) {
    return orderService.lambdaQuery()
        .ge(Orders::getOrderTime, begin.atStartOfDay())
        .le(Orders::getOrderTime, end.atTime(LocalTime.MAX))
        .list();
  }

  /**
   * 查询指定日期范围内的用户.
   *
   * @param begin 开始日期
   * @param end   结束日期
   * @return 用户列表
   */
  private List<User> queryUsersInRange(LocalDate begin, LocalDate end) {
    return userService.lambdaQuery()
        .ge(User::getCreateTime, begin.atStartOfDay())
        .le(User::getCreateTime, end.atTime(LocalTime.MAX))
        .list();
  }

  /**
   * 初始化每日数据映射.
   *
   * @param begin 开始日期
   * @param end   结束日期
   * @return 每日数据映射
   */
  private Map<LocalDate, DailyData> initializeDailyDataMap(LocalDate begin, LocalDate end) {
    Map<LocalDate, DailyData> dailyDataMap = new HashMap<>();
    LocalDate currentDate = begin;
    while (!currentDate.isAfter(end)) {
      dailyDataMap.put(currentDate, new DailyData(currentDate));
      currentDate = currentDate.plusDays(1);
    }
    return dailyDataMap;
  }

  /**
   * 聚合订单数据.
   *
   * @param ordersList   订单列表
   * @param dailyDataMap 每日数据映射
   */
  private void aggregateOrderData(List<Orders> ordersList, Map<LocalDate, DailyData> dailyDataMap) {
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
  }

  /**
   * 聚合用户数据.
   *
   * @param userList     用户列表
   * @param dailyDataMap 每日数据映射
   */
  private void aggregateUserData(List<User> userList, Map<LocalDate, DailyData> dailyDataMap) {
    for (User user : userList) {
      LocalDate createDate = user.getCreateTime().toLocalDate();
      DailyData data = dailyDataMap.get(createDate);
      if (data != null) {
        data.newUsers++;
      }
    }
  }

  /**
   * 概览统计数据.
   */
  private static class OverviewStatistics {
    BigDecimal totalTurnover = BigDecimal.ZERO;
    int totalValidOrders = 0;
    int totalOrders = 0;
    int totalNewUsers = 0;
    double orderCompletionRate = 0.0;
    double unitPrice = 0.0;
  }

  /**
   * 计算概览统计数据.
   *
   * @param dailyDataMap 每日数据映射
   * @return 概览统计数据
   */
  private OverviewStatistics calculateOverviewStatistics(Map<LocalDate, DailyData> dailyDataMap) {
    OverviewStatistics overview = new OverviewStatistics();
    for (DailyData data : dailyDataMap.values()) {
      overview.totalTurnover = overview.totalTurnover.add(data.turnover);
      overview.totalValidOrders += data.validOrders;
      overview.totalOrders += data.totalOrders;
      overview.totalNewUsers += data.newUsers;
    }

    overview.orderCompletionRate =
        overview.totalOrders > 0 ? (double) overview.totalValidOrders / overview.totalOrders : 0.0;
    overview.unitPrice = overview.totalValidOrders > 0
        ? overview.totalTurnover.divide(BigDecimal.valueOf(overview.totalValidOrders), 2,
            RoundingMode.HALF_UP).doubleValue()
        : 0.0;

    return overview;
  }

  /**
   * 生成Excel文件.
   *
   * @param dailyDataMap 每日数据映射
   * @param overview     概览统计数据
   * @param begin        开始日期
   * @param end          结束日期
   * @return Excel文件字节数组
   */
  private byte[] generateExcel(Map<LocalDate, DailyData> dailyDataMap, OverviewStatistics overview,
      LocalDate begin, LocalDate end) {
    ClassPathResource templateResource = new ClassPathResource("template/model.xlsx");

    try (InputStream templateInputStream = templateResource.getInputStream();
        XSSFWorkbook workbook = new XSSFWorkbook(templateInputStream);
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {

      Sheet sheet = workbook.getSheetAt(0);
      fillOverviewData(sheet, overview);
      fillDetailData(sheet, dailyDataMap, begin, end);

      workbook.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      log.error("导出Excel失败", e);
      throw new IllegalStateException("导出Excel失败", e);
    }
  }

  /**
   * 填充概览数据.
   *
   * @param sheet    工作表
   * @param overview 概览统计数据
   */
  private void fillOverviewData(Sheet sheet, OverviewStatistics overview) {
    // 概览数据第一行（原第2行，现在第3行，索引为2）
    Row overviewRow1 = getOrCreateRow(sheet, 3);
    setCellValue(overviewRow1, 2, String.format("%.2f", overview.totalTurnover.doubleValue()));
    setCellValue(overviewRow1, 4, String.format("%.2f%%", overview.orderCompletionRate * 100));
    setCellValue(overviewRow1, 6, String.valueOf(overview.totalNewUsers));

    // 概览数据第二行（原第3行，现在第4行，索引为3）
    Row overviewRow2 = getOrCreateRow(sheet, 4);
    setCellValue(overviewRow2, 2, String.valueOf(overview.totalValidOrders));
    setCellValue(overviewRow2, 4, String.format("%.2f", overview.unitPrice));
  }

  /**
   * 填充明细数据.
   *
   * @param sheet       工作表
   * @param dailyDataMap 每日数据映射
   * @param begin        开始日期
   * @param end          结束日期
   */
  private void fillDetailData(Sheet sheet, Map<LocalDate, DailyData> dailyDataMap, LocalDate begin,
      LocalDate end) {
    int currentRow = 7; // 明细数据开始行
    LocalDate currentDate = begin;

    while (!currentDate.isAfter(end)) {
      DailyData data = dailyDataMap.get(currentDate);
      Row dataRow = getOrCreateRow(sheet, currentRow);

      final double dailyOrderCompletionRate =
          data.totalOrders > 0 ? (double) data.validOrders / data.totalOrders : 0.0;
      final double dailyUnitPrice = data.validOrders > 0
          ? data.turnover.divide(BigDecimal.valueOf(data.validOrders), 2, RoundingMode.HALF_UP)
              .doubleValue()
          : 0.0;

      setCellValue(dataRow, 1, data.date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
      setCellValue(dataRow, 2, String.format("%.2f", data.turnover.doubleValue()));
      setCellValue(dataRow, 3, String.valueOf(data.validOrders));
      setCellValue(dataRow, 4, String.format("%.2f%%", dailyOrderCompletionRate * 100));
      setCellValue(dataRow, 5, String.format("%.2f", dailyUnitPrice));
      setCellValue(dataRow, 6, String.valueOf(data.newUsers));

      currentRow++;
      currentDate = currentDate.plusDays(1);
    }
  }

  /**
   * 获取或创建行.
   *
   * @param sheet 工作表
   * @param rowIndex 行索引
   * @return 行对象
   */
  private Row getOrCreateRow(Sheet sheet, int rowIndex) {
    Row row = sheet.getRow(rowIndex);
    if (row == null) {
      row = sheet.createRow(rowIndex);
    }
    return row;
  }

  /**
   * 构建日期列表字符串.
   *
   * @param dateList 日期列表
   * @return 逗号分隔的日期字符串
   */
  private String buildDateListString(List<LocalDate> dateList) {
    return dateList.stream()
        .map(LocalDate::toString)
        .collect(Collectors.joining(","));
  }

  /**
   * 构建整数列表字符串.
   *
   * @param integerList 整数列表
   * @return 逗号分隔的整数字符串
   */
  private String buildIntegerListString(List<Integer> integerList) {
    return integerList.stream()
        .map(String::valueOf)
        .collect(Collectors.joining(","));
  }

  /**
   * 构建BigDecimal列表字符串.
   *
   * @param bigDecimalList BigDecimal列表
   * @return 逗号分隔的BigDecimal字符串
   */
  private String buildBigDecimalListString(List<BigDecimal> bigDecimalList) {
    return bigDecimalList.stream()
        .map(BigDecimal::toString)
        .collect(Collectors.joining(","));
  }

  /**
   * 验证日期范围.
   *
   * @param begin 开始日期
   * @param end   结束日期
   */
  private void validateDateRange(LocalDate begin, LocalDate end) {
    if (begin == null) {
      throw new IllegalArgumentException("开始日期不能为空");
    }
    if (end == null) {
      throw new IllegalArgumentException("结束日期不能为空");
    }
    if (begin.isAfter(end)) {
      throw new IllegalArgumentException("开始日期不能晚于结束日期");
    }
    // 限制最大查询范围为1年
    if (begin.plusYears(1).isBefore(end) || begin.plusYears(1).equals(end)) {
      throw new IllegalArgumentException("查询日期范围不能超过1年");
    }
  }

  /**
   * 设置单元格值（如果单元格不存在则创建）.
   *
   * @param row    行对象
   * @param column 列索引
   * @param value  单元格值
   */
  private void setCellValue(Row row, int column, String value) {
    Cell cell = row.getCell(column);
    if (cell == null) {
      cell = row.createCell(column);
    }
    cell.setCellValue(value);
  }

  /**
   * 每日数据内部类.
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
