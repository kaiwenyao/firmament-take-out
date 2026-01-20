package dev.kaiwen.controller.admin;

import dev.kaiwen.result.Result;
import dev.kaiwen.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Shop management controller.
 */
@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Tag(name = "店铺相关接口")
@RequiredArgsConstructor
public class ShopController {

  private final ShopService shopService;

  /**
   * Set shop status.
   *
   * @param status The shop status, 1 means open, 0 means closed.
   * @return The operation result, returns success message on success.
   */
  @PutMapping("/{status}")
  @Operation(summary = "设置营业状态")
  public Result<String> setStatus(@PathVariable Integer status) {
    shopService.setStatus(status);
    return Result.success();
  }

  /**
   * Get shop status.
   *
   * @return The shop status, 1 means open, 0 means closed.
   */
  @GetMapping("/status")
  @Operation(summary = "获取店铺营业状态")
  public Result<Integer> getStatus() {
    Integer shopStatus = shopService.getStatus();
    return Result.success(shopStatus);
  }

}
