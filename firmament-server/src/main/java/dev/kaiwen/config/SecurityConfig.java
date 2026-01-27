package dev.kaiwen.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 安全配置类.
 * 用于配置密码编码器等安全相关组件.
 */
@Configuration
public class SecurityConfig {

  /**
   * 配置 BCryptPasswordEncoder Bean.
   * 用于密码加密和验证.
   *
   * @return BCryptPasswordEncoder 实例
   */
  @Bean
  public BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
