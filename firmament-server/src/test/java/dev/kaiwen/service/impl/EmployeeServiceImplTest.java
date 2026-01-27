package dev.kaiwen.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import dev.kaiwen.constant.MessageConstant;
import dev.kaiwen.constant.StatusConstant;
import dev.kaiwen.context.BaseContext;
import dev.kaiwen.dto.EmployeeDto;
import dev.kaiwen.dto.EmployeeLoginDto;
import dev.kaiwen.dto.EmployeePageQueryDto;
import dev.kaiwen.dto.PasswordEditDto;
import dev.kaiwen.entity.Employee;
import dev.kaiwen.exception.AccountLockedException;
import dev.kaiwen.exception.AccountNotFoundException;
import dev.kaiwen.exception.BaseException;
import dev.kaiwen.exception.PasswordEditFailedException;
import dev.kaiwen.exception.PasswordErrorException;
import dev.kaiwen.mapper.EmployeeMapper;
import dev.kaiwen.result.PageResult;
import dev.kaiwen.result.Result;
import dev.kaiwen.utils.PasswordService;
import java.util.Collections;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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

  @Captor
  private ArgumentCaptor<LambdaQueryWrapper<Employee>> wrapperCaptor;

  @Captor
  private ArgumentCaptor<Employee> employeeCaptor;

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
    assertThrows(PasswordErrorException.class, () ->
        employeeService.login(dto)
    );
  }

  @Test
  void loginWithNullAccount() {
    EmployeeLoginDto dto = new EmployeeLoginDto();
    dto.setUsername("admin");
    dto.setPassword("123456");
    when(mapper.selectOne(any())).thenReturn(null);
    assertThrows(AccountNotFoundException.class, () ->
        employeeService.login(dto)
    );
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
    assertThrows(AccountLockedException.class, () ->
        employeeService.login(dto)
    );
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

      // 这里的captor用来捕获保存的对象。

      verify(mapper).insert(employeeCaptor.capture());
      // 这里被insert的对象就会被捕获到captor中
      // verify属于是事后查账。
      // 虽然insert方法早就已经被执行过，但是过程会被记录下来。执行之后我们再调用verify去进行验证也是没有任何问题的。
      // 这就是verify的精髓。

      Employee savedEmployee = employeeCaptor.getValue();

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

      BaseException exception = assertThrows(BaseException.class, () ->
          employeeService.save(dto)
      );

      assertEquals("新增员工失败", exception.getMessage());

    }

  }

  @Test
  void testPageQuerySuccess() {
    // 1. 准备入参
    EmployeePageQueryDto dto = new EmployeePageQueryDto();
    dto.setPage(1);
    dto.setPageSize(10);
    dto.setName("张三"); // 测试条件查询

    // 2. 准备模拟的数据库返回数据
    Employee mockEmployee = new Employee();
    mockEmployee.setId(100L);
    mockEmployee.setName("张三");
    List<Employee> dbRecords = Collections.singletonList(mockEmployee);

    // 当调用 selectPage 时...
    doAnswer(invocation -> {
      // 获取传入的第一个参数 (Page 对象)
      Page<Employee> pageArg = invocation.getArgument(0);

      // 手动往里面填数据（模拟 MP 查库后的行为）
      pageArg.setRecords(dbRecords);
      pageArg.setTotal(100L); // 假设数据库总共有100条

      return pageArg; // selectPage 方法本身的返回值（通常不需要关心）
    }).when(mapper).selectPage(any(), any());

    // 4. 执行测试
    PageResult result = employeeService.pageQuery(dto);

    // 5. 断言结果
    assertEquals(100L, result.getTotal()); // 验证 total 是否正确取出
    assertEquals(1, result.getRecords().size()); // 验证数据是否正确

    verify(mapper).selectPage(any(), wrapperCaptor.capture());

  }

  @Test
  void testEnableSuccess() {
    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(888L);
      when(mapper.update(isNull(), any())).thenReturn(1);

      employeeService.enableOrDisable(StatusConstant.ENABLE, 100L);

      verify(mapper).update(isNull(), any());
    }
  }

  @Test
  void testDisableSuccess() {
    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(888L);
      when(mapper.update(isNull(), any())).thenReturn(1);

      employeeService.enableOrDisable(StatusConstant.DISABLE, 100L);

      verify(mapper).update(isNull(), any());
    }
  }

  @Test
  void testEnableOrDisableFailure() {

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(888L);
      when(mapper.update(isNull(), any())).thenReturn(0);

      assertThrows(AccountNotFoundException.class, () ->
          employeeService.enableOrDisable(StatusConstant.DISABLE, 999L)
      );
    }
  }

  @Test
  void testUpdateSuccess() {
    EmployeeDto dto = new EmployeeDto();
    dto.setId(100L);
    dto.setUsername("updateUser");
    dto.setName("New Name");

    employeeService.update(dto);
    verify(mapper).updateById(employeeCaptor.capture());
    Employee capturedEmployee = employeeCaptor.getValue();
    assertEquals(100L, capturedEmployee.getId());
    assertEquals("updateUser", capturedEmployee.getUsername());
    assertEquals("New Name", capturedEmployee.getName());
  }

  @Test
  void testEditPasswordWhileNoEmployee() {
    PasswordEditDto dto = new PasswordEditDto();
    dto.setEmpId(100L);
    dto.setOldPassword("oldPassword");
    when(mapper.selectOne(any())).thenReturn(null);
    AccountNotFoundException exception = assertThrows(AccountNotFoundException.class, () ->
        employeeService.editPassword(dto)
    );
    assertEquals(MessageConstant.ACCOUNT_NOT_FOUND, exception.getMessage());
  }

  @Test
  void testEditPasswordWhileIncorrectOldPassword() {
    PasswordEditDto dto = new PasswordEditDto();
    dto.setEmpId(100L);
    dto.setOldPassword("oldPassword");
    Employee mockEmployee = new Employee();

    when(mapper.selectOne(any())).thenReturn(mockEmployee);

    when(passwordService.mismatches(dto.getOldPassword(), mockEmployee.getPassword())).thenReturn(
        true);
    PasswordErrorException exception = assertThrows(PasswordErrorException.class, () ->
        employeeService.editPassword(dto)
    );
    assertEquals(MessageConstant.PASSWORD_ERROR, exception.getMessage());
  }

  @Test
  void testEditPasswordFailure() {
    PasswordEditDto dto = new PasswordEditDto();
    dto.setEmpId(100L);
    dto.setOldPassword("oldPassword");
    dto.setNewPassword("newPassword");
    Employee mockEmployee = new Employee();
    mockEmployee.setPassword("storedPassword");

    when(mapper.selectOne(any())).thenReturn(mockEmployee);

    when(passwordService.mismatches(dto.getOldPassword(), mockEmployee.getPassword())).thenReturn(
        false);
    when(passwordService.encode(any())).thenReturn("encrypted_123456");
    when(mapper.update(isNull(), any())).thenReturn(0);
    PasswordEditFailedException exception = assertThrows(PasswordEditFailedException.class, () ->
        employeeService.editPassword(dto));

    assertEquals(MessageConstant.PASSWORD_EDIT_FAILED, exception.getMessage());
    verify(passwordService).encode("newPassword");
  }

  @Test
  void testEditPasswordSuccess() {
    PasswordEditDto dto = new PasswordEditDto();
    dto.setEmpId(100L);
    dto.setOldPassword("oldPassword");
    dto.setNewPassword("newPassword");
    Employee mockEmployee = new Employee();
    mockEmployee.setPassword("storedPassword");

    when(mapper.selectOne(any())).thenReturn(mockEmployee);

    when(passwordService.mismatches(dto.getOldPassword(), mockEmployee.getPassword())).thenReturn(
        false);
    when(passwordService.encode(any())).thenReturn("encrypted_123456");
    when(mapper.update(isNull(), any())).thenReturn(1);
    employeeService.editPassword(dto);
    verify(mapper).update(isNull(), any());
    verify(passwordService).encode("newPassword");
  }

}
