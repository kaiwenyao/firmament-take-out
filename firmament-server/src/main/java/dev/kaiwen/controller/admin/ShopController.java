package dev.kaiwen.controller.admin;


import dev.kaiwen.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * Shop management controller.
 */
@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Tag(name = "店铺相关接口")
@Slf4j
@RequiredArgsConstructor
public class ShopController {

    private final RedisTemplate redisTemplate;
    public static String KEY = "SHOP_STATUS";

    /**
     * Set shop status.
     *
     * @param status The shop status, 1 means open, 0 means closed.
     * @return The operation result, returns success message on success.
     */
    @PutMapping("/{status}")
    @Operation(summary = "设置营业状态")
    public Result setStatus(@PathVariable Integer status) {
        log.info("设置店铺营业状态为 {}", status == 1 ? "营业中" : "打烊中");
        redisTemplate.opsForValue().set(KEY, status);
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
        Integer shopStatus =(Integer) redisTemplate.opsForValue().get(KEY);
        log.info("获取到店铺营业状态为 {}", shopStatus == 1 ? "营业中" : "打烊中");
        return Result.success(shopStatus);
    }


}
