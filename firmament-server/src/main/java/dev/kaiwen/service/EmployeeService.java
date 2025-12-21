package dev.kaiwen.service;

import dev.kaiwen.dto.EmployeeLoginDTO;
import dev.kaiwen.entity.Employee;

public interface EmployeeService {

    /**
     * 员工登录
     * @param employeeLoginDTO
     * @return
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);

}
