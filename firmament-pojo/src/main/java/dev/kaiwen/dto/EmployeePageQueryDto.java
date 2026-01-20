package dev.kaiwen.dto;

import java.io.Serializable;
import lombok.Data;

/**
 * 员工分页查询数据传输对象.
 */
@Data
public class EmployeePageQueryDto implements Serializable {

  // 员工姓名
  private String name;

  // 页码
  private int page;

  // 每页显示记录数
  private int pageSize;

}
