package dev.kaiwen.controller.user;


import dev.kaiwen.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * Shop controller for client side.
 */
@RestController("userShopController")
@RequestMapping("/user/shop")
@Tag(name = "店铺相关接口-用户")
@Slf4j
@RequiredArgsConstructor
public class ShopController {

    private final RedisTemplate redisTemplate;
    public static String KEY = "SHOP_STATUS";

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
