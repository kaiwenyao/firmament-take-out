package dev.kaiwen.dto;

import java.io.Serializable;
import lombok.Data;

/**
 * C端用户登录.
 */
@Data
public class UserLoginDto implements Serializable {

  private String code;

}
