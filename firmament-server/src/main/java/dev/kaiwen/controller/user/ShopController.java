package dev.kaiwen.controller.user;

import dev.kaiwen.result.Result;
import dev.kaiwen.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Shop controller for client side.
 */
@RestController("userShopController")
@RequestMapping("/user/shop")
@Tag(name = "店铺相关接口-用户")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

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
