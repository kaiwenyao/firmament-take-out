package dev.kaiwen.dto;

import java.io.Serializable;
import lombok.Data;

/**
 * 员工数据传输对象.
 */
@Data
public class EmployeeDto implements Serializable {

  private Long id;

  private String username;

  private String name;

  private String phone;

  private String sex;

  private String idNumber;

}
