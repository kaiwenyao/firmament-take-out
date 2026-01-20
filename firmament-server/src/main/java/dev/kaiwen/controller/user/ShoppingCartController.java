package dev.kaiwen.controller.user;

import dev.kaiwen.dto.ShoppingCartDto;
import dev.kaiwen.entity.ShoppingCart;
import dev.kaiwen.result.Result;
import dev.kaiwen.service.ShoppingCartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Shopping cart controller for client side.
 */
@RestController
@Slf4j
@RequestMapping("/user/shoppingCart")
@Tag(name = "C端 购物车接口")
@RequiredArgsConstructor
public class ShoppingCartController {

  private final ShoppingCartService shoppingCartService;

  /**
   * Add item to shopping cart.
   *
   * @param shoppingCartDto The shopping cart data transfer object containing item information.
   * @return The operation result, returns success message on success.
   */
  @PostMapping("/add")
  @Operation(summary = "添加购物车")
  public Result<Void> add(@RequestBody ShoppingCartDto shoppingCartDto) {
    log.info("添加购物车 商品信息: {}", shoppingCartDto);
    shoppingCartService.addShoppingCart(shoppingCartDto);

    return Result.success();
  }

  /**
   * Decrease item quantity in shopping cart.
   *
   * @param shoppingCartDto The shopping cart data transfer object containing item information.
   * @return The operation result, returns success message on success.
   */
  @PostMapping("/sub")
  @Operation(summary = "减少购物车中商品数量")
  public Result<Void> sub(@RequestBody ShoppingCartDto shoppingCartDto) {
    log.info("减少购物车商品数量: {}", shoppingCartDto);
    shoppingCartService.subShoppingCart(shoppingCartDto);
    return Result.success();
  }

  /**
   * Get shopping cart list.
   *
   * @return The list of items in the shopping cart.
   */
  @GetMapping("/list")
  @Operation(summary = "查看购物车")
  public Result<List<ShoppingCart>> list() {
    List<ShoppingCart> list = shoppingCartService.showShoppingCart();
    return Result.success(list);
  }

  /**
   * Clear shopping cart.
   *
   * @return The operation result, returns success message on success.
   */
  @DeleteMapping("/clean")
  @Operation(summary = "清空购物车")
  public Result<Void> cleanShoppingCart() {
    log.info("清空购物车");
    shoppingCartService.cleanShoppingCart();
    return Result.success();
  }
}
