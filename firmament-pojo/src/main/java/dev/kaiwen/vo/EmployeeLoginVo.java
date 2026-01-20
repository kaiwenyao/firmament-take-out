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
@Schema(description = "员工登录返回的数据格式")
public class EmployeeLoginVo implements Serializable {
    // view object
    @Schema(description = "主键值")
    private Long id;

    @Schema(description = "用户名")
    private String userName;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "访问令牌（Access Token，2小时有效）")
    private String token;

    @Schema(description = "刷新令牌（Refresh Token，7天有效，用于获取新的Access Token）")
    private String refreshToken;

}
