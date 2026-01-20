package dev.kaiwen.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "刷新令牌返回的数据格式")
public class RefreshTokenVo implements Serializable {

    @Schema(description = "新的访问令牌（Access Token，2小时有效）")
    private String token;

    @Schema(description = "刷新令牌（Refresh Token，保持不变）")
    private String refreshToken;

}
