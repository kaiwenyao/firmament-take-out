package dev.kaiwen.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import dev.kaiwen.constant.MessageConstant;
import dev.kaiwen.constant.PasswordConstant;
import dev.kaiwen.constant.StatusConstant;
import dev.kaiwen.converter.EmployeeConverter;
import dev.kaiwen.dto.EmployeeDTO;
import dev.kaiwen.dto.EmployeeLoginDTO;
import dev.kaiwen.entity.Employee;
import dev.kaiwen.exception.AccountLockedException;
import dev.kaiwen.exception.AccountNotFoundException;
import dev.kaiwen.exception.PasswordErrorException;
import dev.kaiwen.mapper.EmployeeMapper;
import dev.kaiwen.result.Result;
import dev.kaiwen.service.IEmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee> implements IEmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private EmployeeConverter employeeConverter;
    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        // 1、根据用户名查询数据库中的数据
//        Employee employee = employeeMapper.getByUsername(username);

        // 新写法：使用 MP 的链式查询 (Chain Wrapper)
        Employee employee = lambdaQuery()
                .eq(Employee::getUsername, username) // 等同于 where username = ?
                .one(); // 查询单条数据
        // 2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        // 密码比对
        // 后期需要进行md5加密，然后再进行比对
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }

    /**
     * 新增员工
     * @param employeeDTO
     */
    @Override
    public Result<String> save(EmployeeDTO employeeDTO) {

        // 1. 检查用户名是否已存在
        String username = employeeDTO.getUsername();

        // 2. 转换DTO为Entity
        Employee employee = employeeConverter.d2e(employeeDTO);
        
        // 3. 设置默认值
        employee.setStatus(StatusConstant.ENABLE);
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());
        // TODO 记得获取当前用户
        employee.setCreateUser(10L);
        employee.setUpdateUser(10L);
        
        // 4. 保存到数据库
        boolean saved = save(employee);
        if (!saved) {
            log.error("保存员工失败：{}", employeeDTO);
            throw new RuntimeException("新增员工失败");
        }
        
        log.info("新增员工成功，员工ID：{}", employee.getId());
        return Result.success();
    }
}
