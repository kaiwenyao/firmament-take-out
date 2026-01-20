package dev.kaiwen.service;

import dev.kaiwen.vo.OrderReportVo;
import dev.kaiwen.vo.SalesTop10ReportVo;
import dev.kaiwen.vo.TurnoverReportVo;
import dev.kaiwen.vo.UserReportVo;
import java.time.LocalDate;

/**
 * 报表服务接口.
 */
public interface ReportService {

  /**
   * 获取营业额统计数据.
   *
   * @param begin 开始日期
   * @param end 结束日期
   * @return 营业额报表VO
   */
  TurnoverReportVo getTurnoverStatistics(LocalDate begin, LocalDate end);

  /**
   * 获取用户统计数据.
   *
   * @param begin 开始日期
   * @param end 结束日期
   * @return 用户报表VO
   */
  UserReportVo getUserStatistics(LocalDate begin, LocalDate end);

  /**
   * 获取订单统计数据.
   *
   * @param begin 开始日期
   * @param end 结束日期
   * @return 订单报表VO
   */
  OrderReportVo getOrderStatistics(LocalDate begin, LocalDate end);

  /**
   * 获取销售Top10统计数据.
   *
   * @param begin 开始日期
   * @param end 结束日期
   * @return 销售Top10报表VO
   */
  SalesTop10ReportVo getSalesTop10(LocalDate begin, LocalDate end);

  /**
   * 导出最近30天的数据报表.
   *
   * @return Excel文件的字节数组
   */
  byte[] exportBusinessData();
}

