package dev.kaiwen.controller.user;


import dev.kaiwen.dto.OrdersPaymentDTO;
import dev.kaiwen.dto.OrdersSubmitDTO;
import dev.kaiwen.result.Result;
import dev.kaiwen.service.IOrderService;
import dev.kaiwen.vo.OrderSubmitVO;
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

    private final IOrderService orderService;

    @PostMapping("/submit")
    @Operation(summary = "用户提交订单")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO) {
        log.info("用户下单：{}", ordersSubmitDTO);
        OrderSubmitVO orderSubmitVO = orderService.submitOrder(ordersSubmitDTO);
        return Result.success(orderSubmitVO);

    }

    @PutMapping("/payment")
    @Operation(summary = "订单支付")
    public Result<String> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) {
        log.info("订单支付：{}", ordersPaymentDTO);
        orderService.payment(ordersPaymentDTO);
        return Result.success("支付成功");
    }

}
