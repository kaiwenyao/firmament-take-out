package dev.kaiwen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import dev.kaiwen.dto.OrdersCancelDto;
import dev.kaiwen.dto.OrdersConfirmDto;
import dev.kaiwen.dto.OrdersPageQueryDto;
import dev.kaiwen.dto.OrdersPaymentDto;
import dev.kaiwen.dto.OrdersRejectionDto;
import dev.kaiwen.dto.OrdersSubmitDto;
import dev.kaiwen.entity.Orders;
import dev.kaiwen.result.PageResult;
import dev.kaiwen.vo.OrderStatisticsVo;
import dev.kaiwen.vo.OrderSubmitVo;
import dev.kaiwen.vo.OrderVo;

/**
 * 订单服务接口.
 */
public interface OrderService extends IService<Orders> {

  /**
   * 提交订单.
   *
   * @param ordersSubmitDto 订单提交DTO
   * @return 订单提交VO
   */
  OrderSubmitVo submitOrder(OrdersSubmitDto ordersSubmitDto);

  /**
   * 订单支付.
   *
   * @param ordersPaymentDto 订单支付DTO
   */
  void payment(OrdersPaymentDto ordersPaymentDto);

  /**
   * 处理超时订单.
   */
  void processTimeoutOrder();

  /**
   * 处理前一天未完成的订单：将前一天的所有未完成订单标记为已完成.
   */
  void processDeliveryOrder();

  /**
   * 用户端订单分页查询.
   *
   * @param page     页码
   * @param pageSize 每页大小
   * @param status   订单状态
   * @return 分页结果
   */
  PageResult pageQuery4User(int page, int pageSize, Integer status);

  /**
   * 查询订单详情.
   *
   * @param id 订单ID
   * @return 订单VO
   */
  OrderVo details(Long id);

  /**
   * 用户取消订单.
   *
   * @param id 订单ID
   */
  void userCancelById(Long id);

  /**
   * 用户根据订单号取消订单.
   *
   * @param orderNumber 订单号
   */
  void userCancelByNumber(String orderNumber);

  /**
   * 再来一单.
   *
   * @param id 订单ID
   */
  void repetition(Long id);

  /**
   * 条件搜索订单.
   *
   * @param ordersPageQueryDto 订单分页查询DTO
   * @return 分页结果
   */
  PageResult conditionSearch(OrdersPageQueryDto ordersPageQueryDto);

  /**
   * 各个状态的订单数量统计.
   *
   * @return 订单统计VO
   */
  OrderStatisticsVo statistics();

  /**
   * 接单.
   *
   * @param ordersConfirmDto 订单确认DTO
   */
  void confirm(OrdersConfirmDto ordersConfirmDto);

  /**
   * 拒单.
   *
   * @param ordersRejectionDto 订单拒单DTO
   */
  void rejection(OrdersRejectionDto ordersRejectionDto);

  /**
   * 商家取消订单.
   *
   * @param ordersCancelDto 订单取消DTO
   */
  void cancel(OrdersCancelDto ordersCancelDto);

  /**
   * 派送订单.
   *
   * @param id 订单ID
   */
  void delivery(Long id);

  /**
   * 完成订单.
   *
   * @param id 订单ID
   */
  void complete(Long id);

  /**
   * 催单.
   *
   * @param id 订单ID
   */
  void reminder(Long id);

  /**
   * 根据订单号查询订单详情.
   *
   * @param orderNumber 订单号
   * @return 订单VO
   */
  OrderVo detailsByNumber(String orderNumber);

  /**
   * 根据订单号再来一单.
   *
   * @param orderNumber 订单号
   */
  void repetitionByNumber(String orderNumber);

  /**
   * 根据订单号催单.
   *
   * @param orderNumber 订单号
   */
  void reminderByNumber(String orderNumber);
}
