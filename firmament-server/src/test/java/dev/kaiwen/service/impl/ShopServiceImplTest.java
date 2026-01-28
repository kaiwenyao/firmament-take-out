package dev.kaiwen.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class ShopServiceImplTest {

  @InjectMocks
  private ShopServiceImpl shopService;

  @Mock
  private RedisTemplate<String, Object> redisTemplate;

  @Mock
  private ValueOperations<String, Object> valueOperations;

  @Test
  void setStatusSuccess() {
    // 1. Mock 依赖行为
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);

    // 2. 执行测试
    shopService.setStatus(1);

    // 3. 验证方法调用
    verify(redisTemplate).opsForValue();
    verify(valueOperations).set("SHOP_STATUS", 1);
  }

  @Test
  void setStatusToClosed() {
    // 1. Mock 依赖行为
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);

    // 2. 执行测试 - 测试设置状态为 0（打烊）
    shopService.setStatus(0);

    // 3. 验证方法调用
    verify(redisTemplate).opsForValue();
    verify(valueOperations).set("SHOP_STATUS", 0);
  }

  @Test
  void getStatusSuccess() {
    // 1. Mock 依赖行为
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get("SHOP_STATUS")).thenReturn(1);

    // 2. 执行测试
    Integer result = shopService.getStatus();

    // 3. 验证结果
    assertNotNull(result);
    assertEquals(1, result);
    
    // 4. 验证方法调用
    verify(redisTemplate).opsForValue();
    verify(valueOperations).get("SHOP_STATUS");
  }

  @Test
  void getStatusWithNull() {
    // 1. Mock 依赖行为 - 模拟 Redis 返回 null，触发 else 分支
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get("SHOP_STATUS")).thenReturn(null);

    // 2. 执行测试
    Integer result = shopService.getStatus();

    // 3. 验证结果 - 确保 else 分支被执行，返回默认值 0
    assertNotNull(result);
    assertEquals(0, result); // 默认打烊，验证 else 分支被正确执行
    
    // 4. 验证方法调用
    verify(redisTemplate).opsForValue();
    verify(valueOperations).get("SHOP_STATUS");
  }

  @Test
  void getStatusWithZero() {
    // 1. Mock 依赖行为 - 测试返回 0 的情况
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get("SHOP_STATUS")).thenReturn(0);

    // 2. 执行测试
    Integer result = shopService.getStatus();

    // 3. 验证结果
    assertNotNull(result);
    assertEquals(0, result);
    
    // 4. 验证方法调用
    verify(redisTemplate).opsForValue();
    verify(valueOperations).get("SHOP_STATUS");
  }
}
