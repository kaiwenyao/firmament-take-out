package dev.kaiwen.controller.user;

import dev.kaiwen.dto.ShoppingCartDTO;
import dev.kaiwen.result.Result;
import dev.kaiwen.service.IShoppingCartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/user/shoppingCart")
@Tag(name = "C端 购物车接口")
@RequiredArgsConstructor
public class ShoppingCartController {
    private final IShoppingCartService shoppingCartService;
    @PostMapping("/add")
    @Operation(summary = "添加购物车")
    public Result add(@RequestBody ShoppingCartDTO shoppingCartDTO) {
        log.info("添加购物车 商品信息: {}", shoppingCartDTO);
        shoppingCartService.addShoppingCart(shoppingCartDTO);

        return Result.success();
    }
}
