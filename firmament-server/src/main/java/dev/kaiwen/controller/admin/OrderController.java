package dev.kaiwen.controller.admin;

import dev.kaiwen.dto.OrdersCancelDto;
import dev.kaiwen.dto.OrdersConfirmDto;
import dev.kaiwen.dto.OrdersPageQueryDto;
import dev.kaiwen.dto.OrdersRejectionDto;
import dev.kaiwen.result.PageResult;
import dev.kaiwen.result.Result;
import dev.kaiwen.service.OrderService;
import dev.kaiwen.vo.OrderStatisticsVO;
import dev.kaiwen.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Order management controller.
 */
@RestController("adminOrderController")
@RequestMapping("/admin/order")
@Slf4j
@Tag(name = "订单管理接口")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Search orders with conditions.
     *
     * @param ordersPageQueryDto The order page query conditions, including page number, page size,
     *                           order number, phone number, status, begin time, end time and other query parameters.
     * @return The page query result containing order list and pagination information.
     */
    @GetMapping("/conditionSearch")
    @Operation(summary = "订单搜索")
    public Result<PageResult> conditionSearch(OrdersPageQueryDto ordersPageQueryDto) {
        PageResult pageResult = orderService.conditionSearch(ordersPageQueryDto);
        return Result.success(pageResult);
    }

    /**
     * Get order statistics by status.
     *
     * @return The order statistics containing order counts for each status.
     */
    @GetMapping("/statistics")
    @Operation(summary = "各个状态的订单数量统计")
    public Result<OrderStatisticsVO> statistics() {
        OrderStatisticsVO orderStatisticsVO = orderService.statistics();
        return Result.success(orderStatisticsVO);
    }

    /**
     * Get order details by ID.
     *
     * @param id The order ID.
     * @return The order detailed information.
     */
    @GetMapping("/details/{id}")
    @Operation(summary = "查询订单详情")
    public Result<OrderVO> details(@PathVariable Long id) {
        OrderVO orderVO = orderService.details(id);
        return Result.success(orderVO);
    }

    /**
     * Confirm order.
     *
     * @param ordersConfirmDto The order confirm data transfer object containing order ID.
     * @return The operation result, returns success message on success.
     */
    @PutMapping("/confirm")
    @Operation(summary = "接单")
    public Result<String> confirm(@RequestBody OrdersConfirmDto ordersConfirmDto) {
        orderService.confirm(ordersConfirmDto);
        return Result.success();
    }

    /**
     * Reject order.
     *
     * @param ordersRejectionDto The order rejection data transfer object containing order ID and rejection reason.
     * @return The operation result, returns success message on success.
     * @throws Exception If rejection fails.
     */
    @PutMapping("/rejection")
    @Operation(summary = "拒单")
    public Result<String> rejection(@RequestBody OrdersRejectionDto ordersRejectionDto) throws Exception {
        orderService.rejection(ordersRejectionDto);
        return Result.success();
    }

    /**
     * Cancel order.
     *
     * @param ordersCancelDto The order cancel data transfer object containing order ID and cancel reason.
     * @return The operation result, returns success message on success.
     * @throws Exception If cancellation fails.
     */
    @PutMapping("/cancel")
    @Operation(summary = "取消订单")
    public Result<String> cancel(@RequestBody OrdersCancelDto ordersCancelDto) throws Exception {
        orderService.cancel(ordersCancelDto);
        return Result.success();
    }

    /**
     * Deliver order.
     *
     * @param id The order ID.
     * @return The operation result, returns success message on success.
     */
    @PutMapping("/delivery/{id}")
    @Operation(summary = "派送订单")
    public Result<String> delivery(@PathVariable Long id) {
        orderService.delivery(id);
        return Result.success();
    }

    /**
     * Complete order.
     *
     * @param id The order ID.
     * @return The operation result, returns success message on success.
     */
    @PutMapping("/complete/{id}")
    @Operation(summary = "完成订单")
    public Result<String> complete(@PathVariable Long id) {
        orderService.complete(id);
        return Result.success();
    }
}

