package dev.kaiwen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import dev.kaiwen.dto.EmployeeDTO;
import dev.kaiwen.dto.EmployeeLoginDTO;
import dev.kaiwen.dto.EmployeePageQueryDTO;
import dev.kaiwen.entity.Employee;
import dev.kaiwen.result.PageResult;
import dev.kaiwen.result.Result;

public interface EmployeeService extends IService<Employee> {

    /**
     * 员工登录
     * @param employeeLoginDTO
     * @return
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);

    /**
     * 新增员工
     * @param employeeDTO
     * @return
     */
    Result<String> save(EmployeeDTO employeeDTO);

    PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO);

    void enableOrDisable(Integer status, Long employeeId);

    void update(EmployeeDTO employeeDTO);
}

