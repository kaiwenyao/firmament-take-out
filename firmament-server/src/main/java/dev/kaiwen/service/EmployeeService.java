package dev.kaiwen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import dev.kaiwen.dto.EmployeeDto;
import dev.kaiwen.dto.EmployeeLoginDto;
import dev.kaiwen.dto.EmployeePageQueryDto;
import dev.kaiwen.dto.PasswordEditDto;
import dev.kaiwen.entity.Employee;
import dev.kaiwen.result.PageResult;
import dev.kaiwen.result.Result;

public interface EmployeeService extends IService<Employee> {

    /**
     * 员工登录
     * @param employeeLoginDTO
     * @return
     */
    Employee login(EmployeeLoginDto employeeLoginDTO);

    /**
     * 新增员工
     * @param employeeDTO
     * @return
     */
    Result<String> save(EmployeeDto employeeDTO);

    PageResult pageQuery(EmployeePageQueryDto employeePageQueryDTO);

    void enableOrDisable(Integer status, Long employeeId);

    void update(EmployeeDto employeeDTO);

    /**
     * 修改密码
     * @param passwordEditDTO
     */
    void editPassword(PasswordEditDto passwordEditDTO);
}

