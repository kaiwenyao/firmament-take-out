package dev.kaiwen.controller.user;

import dev.kaiwen.dto.ShoppingCartDTO;
import dev.kaiwen.entity.ShoppingCart;
import dev.kaiwen.result.Result;
import dev.kaiwen.service.ShoppingCartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/user/shoppingCart")
@Tag(name = "C端 购物车接口")
@RequiredArgsConstructor
public class ShoppingCartController {
    private final ShoppingCartService shoppingCartService;

    @PostMapping("/add")
    @Operation(summary = "添加购物车")
    public Result<?> add(@RequestBody ShoppingCartDTO shoppingCartDTO) {
        log.info("添加购物车 商品信息: {}", shoppingCartDTO);
        shoppingCartService.addShoppingCart(shoppingCartDTO);

        return Result.success();
    }

    @PostMapping("/sub")
    @Operation(summary = "减少购物车中商品数量")
    public Result<?> sub(@RequestBody ShoppingCartDTO shoppingCartDTO) {
        log.info("减少购物车商品数量: {}", shoppingCartDTO);
        shoppingCartService.subShoppingCart(shoppingCartDTO);
        return Result.success();
    }

    @GetMapping("/list")
    @Operation(summary = "查看购物车")
    public Result<List<ShoppingCart>> list() {
        List<ShoppingCart> list = shoppingCartService.showShoppingCart();
        return Result.success(list);
    }

    /**
     * 清空购物车
     * @return
     */
    @DeleteMapping("/clean")
    @Operation(summary = "清空购物车")
    public Result<?> cleanShoppingCart() {
        log.info("清空购物车");
        shoppingCartService.cleanShoppingCart();
        return Result.success();
    }
}
