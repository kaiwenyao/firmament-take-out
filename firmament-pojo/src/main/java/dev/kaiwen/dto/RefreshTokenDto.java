package dev.kaiwen.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "刷新令牌请求数据模型")
public class RefreshTokenDto implements Serializable {

    @Schema(description = "刷新令牌（Refresh Token）", required = true)
    private String refreshToken;

}
