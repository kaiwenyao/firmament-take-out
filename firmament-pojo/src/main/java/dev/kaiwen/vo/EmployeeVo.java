package dev.kaiwen.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 员工视图对象.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "员工返回的数据格式")
public class EmployeeVo {

  private Long id;

  private String username;

  private String name;

  private String phone;

  private String sex;

  private String idNumber;

  private Integer status;

  private LocalDateTime updateTime;
}
