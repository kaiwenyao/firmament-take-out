package dev.kaiwen.service.impl;

import dev.kaiwen.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 店铺服务实现类.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShopServiceImpl implements ShopService {

  private static final String KEY = "SHOP_STATUS";
  private final RedisTemplate<String, Object> redisTemplate;

  @Override
  public void setStatus(Integer status) {
    log.info("设置店铺营业状态为 {}", status == 1 ? "营业中" : "打烊中");
    redisTemplate.opsForValue().set(KEY, status);
  }

  @Override
  public Integer getStatus() {
    Integer shopStatus = (Integer) redisTemplate.opsForValue().get(KEY);
    if (shopStatus != null) {
      log.info("获取到店铺营业状态为 {}", shopStatus == 1 ? "营业中" : "打烊中");
    } else {
      log.warn("获取店铺营业状态为空，返回默认值 0（打烊中）");
      shopStatus = 0; // 默认打烊
    }
    return shopStatus;
  }
}
