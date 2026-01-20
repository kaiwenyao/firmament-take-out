package dev.kaiwen.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT配置属性.
 */
@Component
@ConfigurationProperties(prefix = "firmament.jwt")
@Data
public class JwtProperties {

  /**
   * 管理端员工生成jwt令牌相关配置.
   */
  private String adminSecretKey;
  // Access Token 有效期（默认2小时）
  private long adminTtl;
  // Refresh Token 有效期（默认7天）
  private long adminRefreshTtl;
  private String adminTokenName;

  /**
   * 用户端微信用户生成jwt令牌相关配置.
   */
  private String userSecretKey;
  // Access Token 有效期（默认2小时）
  private long userTtl;
  // Refresh Token 有效期（默认7天）
  private long userRefreshTtl;
  private String userTokenName;

}
