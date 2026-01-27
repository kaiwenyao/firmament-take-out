package dev.kaiwen.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import dev.kaiwen.constant.StatusConstant;
import dev.kaiwen.context.BaseContext;
import dev.kaiwen.dto.EmployeeDto;
import dev.kaiwen.dto.EmployeeLoginDto;
import dev.kaiwen.entity.Employee;
import dev.kaiwen.exception.AccountLockedException;
import dev.kaiwen.exception.AccountNotFoundException;
import dev.kaiwen.exception.BaseException;
import dev.kaiwen.exception.PasswordErrorException;
import dev.kaiwen.mapper.EmployeeMapper;
import dev.kaiwen.result.Result;
import dev.kaiwen.utils.PasswordService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

  @InjectMocks
  private EmployeeServiceImpl employeeService;

  @Mock
  private EmployeeMapper mapper; // 对应代码中的 mapper

  @Mock
  private PasswordService passwordService; // 对应代码中的 passwordService

  @BeforeEach
  void setUp() {
    TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
        Employee.class);
  }

  @Test
  void loginWithCorrectPassword() {
    EmployeeLoginDto dto = new EmployeeLoginDto();
    dto.setUsername("admin");
    dto.setPassword("123456");

    Employee employee = new Employee();
    employee.setId(1L);
    employee.setStatus(1);
    employee.setUsername("admin");
    employee.setPassword("123456");

    when(mapper.selectOne(any())).thenReturn(employee);
    when(passwordService.mismatches("123456", employee.getPassword())).thenReturn(false);
    when(passwordService.isMd5(employee.getPassword())).thenReturn(false);
    Employee result = employeeService.login(dto);
    // 验证返回结果
    assertNotNull(result);
    assertEquals(1L, result.getId());
    assertEquals("admin", result.getUsername());
    assertEquals(1, result.getStatus());

  }

  @Test
  void loginWithWrongPassword() {
    EmployeeLoginDto dto = new EmployeeLoginDto();
    dto.setUsername("admin");
    dto.setPassword("wrong_password");
    Employee employee = new Employee();
    employee.setPassword("correct_password");
    when(mapper.selectOne(any())).thenReturn(employee);
    when(passwordService.mismatches("wrong_password", employee.getPassword())).thenReturn(true);
    assertThrows(PasswordErrorException.class, () -> {
      employeeService.login(dto);
    });
  }

  @Test
  void loginWithNullAccount() {
    EmployeeLoginDto dto = new EmployeeLoginDto();
    dto.setUsername("admin");
    dto.setPassword("123456");
    when(mapper.selectOne(any())).thenReturn(null);
    assertThrows(AccountNotFoundException.class, () -> {
      employeeService.login(dto);
    });
  }

  @Test
  void loginWithDeactivatedAccount() {
    EmployeeLoginDto dto = new EmployeeLoginDto();
    dto.setUsername("admin");
    dto.setPassword("correct_password");
    Employee employee = new Employee();
    employee.setStatus(0);
    employee.setPassword("correct_password");
    when(mapper.selectOne(any())).thenReturn(employee);
    when(passwordService.mismatches(dto.getPassword(), employee.getPassword())).thenReturn(false);
    assertThrows(AccountLockedException.class, () -> {
      employeeService.login(dto);
    });
  }

  @Test
  void loginWithOldPasswordAndUpdateSuccess() {
    EmployeeLoginDto dto = new EmployeeLoginDto();
    dto.setUsername("admin");
    dto.setPassword("123456");
    Employee employee = new Employee();
    employee.setId(1L); // 给个ID，因为updateWrapper用了getId()
    employee.setPassword("123456");
    employee.setStatus(1); // 保证账号未被锁定
    when(mapper.selectOne(any())).thenReturn(employee);
    when(passwordService.mismatches("123456", employee.getPassword())).thenReturn(false);
    when(passwordService.isMd5(employee.getPassword())).thenReturn(true);
    when(passwordService.encode(employee.getPassword())).thenReturn("encrypted_password");
    when(mapper.update(any(), any())).thenReturn(1);
    Employee result = employeeService.login(dto);
    assertEquals("encrypted_password", result.getPassword());
    verify(mapper).update(any(), any());
  }

  @Test
  void loginWithOldPasswordAndUpdateFailure() {
    EmployeeLoginDto dto = new EmployeeLoginDto();
    dto.setUsername("admin");
    dto.setPassword("123456");
    Employee employee = new Employee();
    employee.setId(1L); // 给个ID，因为updateWrapper用了getId()
    employee.setPassword("123456");
    employee.setStatus(1); // 保证账号未被锁定
    when(mapper.selectOne(any())).thenReturn(employee);
    when(passwordService.mismatches("123456", employee.getPassword())).thenReturn(false);
    when(passwordService.isMd5(employee.getPassword())).thenReturn(true);
    when(passwordService.encode(employee.getPassword())).thenReturn("encrypted_password");
    when(mapper.update(any(), any())).thenReturn(0);
    Employee result = employeeService.login(dto);
    assertEquals("123456", result.getPassword());
    verify(mapper).update(any(), any());
  }

  @Test
  void saveSuccess() {

    EmployeeDto employeeDto = new EmployeeDto();
    employeeDto.setUsername("admin");

    when(passwordService.encode(any())).thenReturn("bcrypt_123456");
    when(mapper.insert(any(Employee.class))).thenReturn(1);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {

      baseContext.when(BaseContext::getCurrentId).thenReturn(888L);

      Result<String> result = employeeService.save(employeeDto);

      assertEquals(1, result.getCode()); // 假设 1 是成功码

      ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
      // 这里的captor用来捕获保存的对象。

      verify(mapper).insert(captor.capture());
      // 这里被insert的对象就会被捕获到captor中
      // verify属于是事后查账。
      // 虽然insert方法早就已经被执行过，但是过程会被记录下来。执行之后我们再调用verify去进行验证也是没有任何问题的。
      // 这就是verify的精髓。

      Employee savedEmployee = captor.getValue();

      assertEquals("bcrypt_123456", savedEmployee.getPassword());

      assertEquals(StatusConstant.ENABLE, savedEmployee.getStatus());

      assertEquals(888L, savedEmployee.getCreateUser());
      assertEquals(888L, savedEmployee.getUpdateUser());

      assertNotNull(savedEmployee.getCreateTime());
      assertNotNull(savedEmployee.getUpdateTime());
    }
  }

  @Test
  void saveFailure() {
    EmployeeDto dto = new EmployeeDto();
    dto.setUsername("admin"); // 最好给点基础数据

    when(passwordService.encode(any())).thenReturn("123456");
    when(mapper.insert(any(Employee.class))).thenReturn(0);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {

      baseContext.when(BaseContext::getCurrentId).thenReturn(1L);

      BaseException exception = assertThrows(BaseException.class, () -> {
        employeeService.save(dto);
      });

      assertEquals("新增员工失败", exception.getMessage());

    }

  }

  @Test
  void pageQuery() {
  }

  @Test
  void enableOrDisable() {
  }

  @Test
  void update() {
  }

  @Test
  void editPassword() {
  }
}