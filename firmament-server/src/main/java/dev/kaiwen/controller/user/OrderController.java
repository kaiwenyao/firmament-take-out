package dev.kaiwen.controller.user;

import dev.kaiwen.dto.OrdersPaymentDto;
import dev.kaiwen.dto.OrdersSubmitDto;
import dev.kaiwen.result.PageResult;
import dev.kaiwen.result.Result;
import dev.kaiwen.service.OrderService;
import dev.kaiwen.vo.OrderSubmitVo;
import dev.kaiwen.vo.OrderVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Order controller for client side.
 */
@Slf4j
@RestController("userOrderController")
@RequestMapping("/user/order")
@RequiredArgsConstructor
@Tag(name = "用户端订单")
public class OrderController {

  private final OrderService orderService;

  /**
   * Remind order by order number.
   *
   * @param orderNumber The order number.
   * @return The operation result, returns success message on success.
   */
  @GetMapping("/reminder/number/{orderNumber}")
  @Operation(summary = "催单-订单号")
  public Result<Void> reminderByNumber(@PathVariable String orderNumber) {
    log.info("用户催单(订单号)：{}", orderNumber);
    orderService.reminderByNumber(orderNumber);
    return Result.success();
  }

  /**
   * Submit order.
   *
   * @param ordersSubmitDto The order submit data transfer object containing order information.
   * @return The order submit result containing order number and order amount.
   */
  @PostMapping("/submit")
  @Operation(summary = "用户提交订单")
  public Result<OrderSubmitVo> submit(@RequestBody OrdersSubmitDto ordersSubmitDto) {
    log.info("用户下单：{}", ordersSubmitDto);
    OrderSubmitVo orderSubmitVo = orderService.submitOrder(ordersSubmitDto);
    return Result.success(orderSubmitVo);

  }

  /**
   * Pay for order.
   *
   * @param ordersPaymentDto The order payment data transfer object containing order number and
   *                         payment method.
   * @return The operation result, returns success message on success.
   */
  @PutMapping("/payment")
  @Operation(summary = "订单支付")
  public Result<String> payment(@RequestBody OrdersPaymentDto ordersPaymentDto) {
    log.info("订单支付：{}", ordersPaymentDto);
    orderService.payment(ordersPaymentDto);
    return Result.success("支付成功");
  }

  /**
   * Get order history with pagination.
   *
   * @param page     The page number.
   * @param pageSize The page size.
   * @param status   The order status, 1 pending payment, 2 pending acceptance, 3 accepted, 4
   *                 delivering, 5 completed, 6 cancelled.
   * @return The page query result containing order list and pagination information.
   */
  @GetMapping("/historyOrders")
  @Operation(summary = "历史订单查询")
  public Result<PageResult> page(int page, int pageSize, Integer status) {
    PageResult pageResult = orderService.pageQuery4User(page, pageSize, status);
    return Result.success(pageResult);
  }

  /**
   * Get order details by order number.
   *
   * @param orderNumber The order number.
   * @return The order detailed information.
   */
  @GetMapping("/orderDetail/number/{orderNumber}")
  @Operation(summary = "查询订单详情-订单号")
  public Result<OrderVo> detailsByNumber(@PathVariable String orderNumber) {
    OrderVo orderVo = orderService.detailsByNumber(orderNumber);
    return Result.success(orderVo);
  }

  /**
   * Cancel order by order number.
   *
   * @param orderNumber The order number.
   * @return The operation result, returns success message on success.
   * @throws Exception If cancellation fails.
   */
  @PutMapping("/cancel/number/{orderNumber}")
  @Operation(summary = "取消订单-订单号")
  public Result<String> cancelByNumber(@PathVariable String orderNumber) throws Exception {
    orderService.userCancelByNumber(orderNumber);
    return Result.success();
  }

  /**
   * Repeat order by order number.
   *
   * @param orderNumber The order number.
   * @return The operation result, returns success message on success.
   */
  @PostMapping("/repetition/number/{orderNumber}")
  @Operation(summary = "再来一单-订单号")
  public Result<String> repetitionByNumber(@PathVariable String orderNumber) {
    orderService.repetitionByNumber(orderNumber);
    return Result.success();
  }

}
