package dev.kaiwen.dto;

import java.io.Serializable;
import lombok.Data;

/**
 * C端用户手机号密码登录DTO.
 */
@Data
public class UserPhoneLoginDto implements Serializable {

  // 手机号
  private String phone;

  // 密码
  private String password;

}

