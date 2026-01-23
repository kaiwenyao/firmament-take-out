package dev.kaiwen.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
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
import org.mockito.Mockito;
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
    TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Employee.class);
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

    // --- 1. 准备数据 (Given) ---
    EmployeeDto employeeDto = new EmployeeDto();
    employeeDto.setUsername("admin");
    // 假设这是前端传来的数据，通常不含密码，业务层用默认密码

    // --- 2. Mock 规则 (When) ---

    // A. Mock 密码服务（必须 mock，否则密码是 null）
    when(passwordService.encode(any())).thenReturn("bcrypt_123456");

    // B. Mock 数据库插入
    // 这里的 1 代表插入成功
    doReturn(1).when(mapper).insert(any(Employee.class));

    // C. Mock 静态上下文 (如果不 mock，User ID 可能是 null)
    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(888L);

      // --- 3. 执行业务 (Execute) ---
      Result<String> result = employeeService.save(employeeDto);

      // --- 4. 基础断言 (Then - Result) ---
      // 验证外层结果是否成功
      assertEquals(1, result.getCode()); // 假设 1 是成功码

      // --- 5. 核心验证：抓取内部对象 (Verification & Captor) ---

      // 【关键】定义一个捕获器，专门抓 Employee 类型的参数
      ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);

      // 验证：mapper.insert 必须被调用一次，并且把参数“抓”进 captor 里
      verify(mapper).insert(captor.capture());

      // 取出抓到的对象（这就是 save 方法内部生成的那个 employee！）
      Employee savedEmployee = captor.getValue();

      // --- 6. 验证业务逻辑是否生效 ---

      // 验证 1: 密码是否被加密了？(而不是 null)
      assertEquals("bcrypt_123456", savedEmployee.getPassword());

      // 验证 2: 状态是否默认设为了 ENABLE？
      assertEquals(StatusConstant.ENABLE, savedEmployee.getStatus());

      // 验证 3: 创建人 ID 是否设为了 BaseContext 里的值？
      assertEquals(888L, savedEmployee.getCreateUser());
      assertEquals(888L, savedEmployee.getUpdateUser());

      // 验证 4: 时间是否被填充了？
      assertNotNull(savedEmployee.getCreateTime());
      assertNotNull(savedEmployee.getUpdateTime());
    }
  }

  @Test
  void saveFailure() {
    EmployeeDto dto = new EmployeeDto();
    dto.setUsername("admin"); // 最好给点基础数据
    // 1. 准备 Mock 环境
    // 虽然是测失败，但前面的加密、获取用户ID流程必须得能跑通
    when(passwordService.encode(any())).thenReturn("123456");

    // 2. 核心 Mock：模拟插入失败 (返回 0)
    // 依然记得用 any(Employee.class) 避免歧义
    doReturn(0).when(mapper).insert(any(Employee.class));

    // 3. 静态 Mock (必不可少)
    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(1L);

      // 4. 执行并验证
      // 现在前面的障碍都清除了，代码会顺利走到 mapper.insert，
      // 拿到 0，然后抛出 BaseException，被这里捕获。
      assertThrows(BaseException.class, () -> {
        employeeService.save(dto);
      });
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