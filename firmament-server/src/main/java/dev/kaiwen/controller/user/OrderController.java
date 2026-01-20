package dev.kaiwen.controller.user;


import dev.kaiwen.dto.OrdersPaymentDto;
import dev.kaiwen.dto.OrdersSubmitDto;
import dev.kaiwen.result.PageResult;
import dev.kaiwen.result.Result;
import dev.kaiwen.service.OrderService;
import dev.kaiwen.vo.OrderSubmitVO;
import dev.kaiwen.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController("userOrderController")
@RequestMapping("/user/order")
@RequiredArgsConstructor
@Tag(name = "用户端订单")
public class OrderController {

    private final OrderService orderService;


    @GetMapping("/reminder/number/{orderNumber}")
    @Operation(summary = "催单-订单号")
    public Result reminderByNumber(@PathVariable String orderNumber) {
        log.info("用户催单(订单号)：{}", orderNumber);
        orderService.reminderByNumber(orderNumber);
        return Result.success();
    }


    @PostMapping("/submit")
    @Operation(summary = "用户提交订单")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDto ordersSubmitDTO) {
        log.info("用户下单：{}", ordersSubmitDTO);
        OrderSubmitVO orderSubmitVO = orderService.submitOrder(ordersSubmitDTO);
        return Result.success(orderSubmitVO);

    }

    @PutMapping("/payment")
    @Operation(summary = "订单支付")
    public Result<String> payment(@RequestBody OrdersPaymentDto ordersPaymentDTO) {
        log.info("订单支付：{}", ordersPaymentDTO);
        orderService.payment(ordersPaymentDTO);
        return Result.success("支付成功");
    }

    /**
     * 历史订单查询
     *
     * @param page
     * @param pageSize
     * @param status   订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
     * @return
     */
    @GetMapping("/historyOrders")
    @Operation(summary = "历史订单查询")
    public Result<PageResult> page(int page, int pageSize, Integer status) {
        PageResult pageResult = orderService.pageQuery4User(page, pageSize, status);
        return Result.success(pageResult);
    }

    @GetMapping("/orderDetail/number/{orderNumber}")
    @Operation(summary = "查询订单详情-订单号")
    public Result<OrderVO> detailsByNumber(@PathVariable String orderNumber) {
        OrderVO orderVO = orderService.detailsByNumber(orderNumber);
        return Result.success(orderVO);
    }

    /**
     * 用户取消订单
     *
     * @return
     */
    @PutMapping("/cancel/number/{orderNumber}")
    @Operation(summary = "取消订单-订单号")
    public Result<String> cancelByNumber(@PathVariable String orderNumber) throws Exception {
        orderService.userCancelByNumber(orderNumber);
        return Result.success();
    }

    @PostMapping("/repetition/number/{orderNumber}")
    @Operation(summary = "再来一单-订单号")
    public Result<String> repetitionByNumber(@PathVariable String orderNumber) {
        orderService.repetitionByNumber(orderNumber);
        return Result.success();
    }

}
