package dev.kaiwen.dto;

import java.io.Serializable;
import lombok.Data;

/**
 * 密码修改数据传输对象.
 */
@Data
public class PasswordEditDto implements Serializable {

  // 员工id
  private Long empId;

  // 旧密码
  private String oldPassword;

  // 新密码
  private String newPassword;

}
