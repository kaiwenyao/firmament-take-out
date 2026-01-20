package dev.kaiwen.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 员工实体类.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("employee")
public class Employee implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;
  @TableId(type = IdType.AUTO)
  private Long id;

  private String username;

  private String name;

  private String password;

  private String phone;

  private String sex;

  private String idNumber;

  private Integer status;

  // @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  // 插入时自动填充
  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createTime;
  // 插入 和 更新 时都自动填充
  @TableField(fill = FieldFill.INSERT_UPDATE)
  // @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime updateTime;
  @TableField(fill = FieldFill.INSERT)
  private Long createUser;
  @TableField(fill = FieldFill.INSERT_UPDATE)
  private Long updateUser;

}
