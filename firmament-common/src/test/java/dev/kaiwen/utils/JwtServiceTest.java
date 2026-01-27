package dev.kaiwen.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.jsonwebtoken.Claims;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  private JwtService jwtService;

  @BeforeEach
  void setUp() {
    jwtService = new JwtService();
  }

  @Test
  void createAndParseJwt() {
    String secretKey = "itcast-firmament-server-secret-key-for-test";

    long ttlMillis = 1000 * 60 * 60; // 1小时
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", 100L);
    claims.put("username", "kaiwen");
    String token = jwtService.createJwt(secretKey, ttlMillis, claims);
    assertNotNull(token);
    System.out.println("生成的 Token: " + token);

    // 3. 测试解析 (Parse)
    // 用刚才生成的 token 马上反向解析，验证闭环逻辑
    Claims parsedClaims = jwtService.parseJwt(secretKey, token);

    // 4. 验证数据一致性
    assertEquals(100, parsedClaims.get("userId", Integer.class).longValue()); // 注意数字类型的转换
    assertEquals("kaiwen", parsedClaims.get("username"));
  }

}