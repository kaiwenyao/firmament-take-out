package dev.kaiwen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import dev.kaiwen.dto.EmployeeDto;
import dev.kaiwen.dto.EmployeeLoginDto;
import dev.kaiwen.dto.EmployeePageQueryDto;
import dev.kaiwen.dto.PasswordEditDto;
import dev.kaiwen.entity.Employee;
import dev.kaiwen.result.PageResult;
import dev.kaiwen.result.Result;

/**
 * 员工服务接口.
 */
public interface EmployeeService extends IService<Employee> {

  /**
   * 员工登录.
   *
   * @param employeeLoginDto 员工登录DTO
   * @return 员工实体
   */
  Employee login(EmployeeLoginDto employeeLoginDto);

  /**
   * 新增员工.
   *
   * @param employeeDto 员工DTO
   * @return 结果
   */
  Result<String> save(EmployeeDto employeeDto);

  /**
   * 分页查询员工.
   *
   * @param employeePageQueryDto 员工分页查询DTO
   * @return 分页结果
   */
  PageResult pageQuery(EmployeePageQueryDto employeePageQueryDto);

  /**
   * 启用、禁用员工.
   *
   * @param status     状态
   * @param employeeId 员工ID
   */
  void enableOrDisable(Integer status, Long employeeId);

  /**
   * 更新员工信息.
   *
   * @param employeeDto 员工DTO
   */
  void update(EmployeeDto employeeDto);

  /**
   * 修改密码.
   *
   * @param passwordEditDto 密码修改DTO
   */
  void editPassword(PasswordEditDto passwordEditDto);
}

