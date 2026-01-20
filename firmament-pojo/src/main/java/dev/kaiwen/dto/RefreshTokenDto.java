package dev.kaiwen.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.Data;

/**
 * 刷新令牌数据传输对象.
 */
@Data
@Schema(description = "刷新令牌请求数据模型")
public class RefreshTokenDto implements Serializable {

  @Schema(description = "刷新令牌（Refresh Token）", requiredMode = Schema.RequiredMode.REQUIRED)
  private String refreshToken;

}
