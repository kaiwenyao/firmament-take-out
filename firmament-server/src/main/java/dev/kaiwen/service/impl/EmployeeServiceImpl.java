package dev.kaiwen.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import dev.kaiwen.constant.MessageConstant;
import dev.kaiwen.constant.PasswordConstant;
import dev.kaiwen.constant.StatusConstant;
import dev.kaiwen.context.BaseContext;
import dev.kaiwen.converter.EmployeeConverter;
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
import dev.kaiwen.service.EmployeeService;
import dev.kaiwen.utils.PasswordUtil;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 员工服务实现类.
 */
@Slf4j
@Service
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee> implements
    EmployeeService {

  /**
   * 员工登录.
   *
   * @param employeeLoginDto 员工登录DTO
   * @return 员工实体
   */
  @Override
  public Employee login(EmployeeLoginDto employeeLoginDto) {
    String username = employeeLoginDto.getUsername();
    String password = employeeLoginDto.getPassword();

    // 1、根据用户名查询数据库中的数据
    // 使用 Mybatis-Plus 的链式查询 (Chain Wrapper)
    Employee employee = lambdaQuery()
        .eq(Employee::getUsername, username) // 等同于 where username = ?
        .one(); // 查询单条数据
    // 2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
    if (employee == null) {
      // 账号不存在
      throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
    }

    // 密码比对
    // 使用PasswordUtil支持BCrypt和MD5两种格式，自动识别
    if (!PasswordUtil.matches(password, employee.getPassword())) {
      // 密码错误
      throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
    }

    // ⭐ 安全优化：如果密码是MD5格式，自动升级为BCrypt
    if (PasswordUtil.isMD5(employee.getPassword())) {
      log.info("检测到员工 {} 使用MD5密码，正在自动升级为BCrypt加密", username);

      // 使用BCrypt重新加密密码
      String bcryptPassword = PasswordUtil.encode(password);

      // 更新数据库中的密码
      boolean updated = lambdaUpdate()
          .eq(Employee::getId, employee.getId())
          .set(Employee::getPassword, bcryptPassword)
          .set(Employee::getUpdateTime, LocalDateTime.now())
          .set(Employee::getUpdateUser, employee.getId())  // 自己更新自己的密码
          .update();

      if (updated) {
        log.info("员工 {} 的密码已成功升级为BCrypt加密格式", username);
        // 更新内存中的employee对象，确保后续逻辑使用最新密码
        employee.setPassword(bcryptPassword);
      } else {
        log.warn("员工 {} 的密码升级失败，但不影响本次登录", username);
      }
    }

    if (StatusConstant.DISABLE.equals(employee.getStatus())) {
      // 账号被锁定
      throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
    }

    // 3、返回实体对象
    return employee;
  }

  /**
   * 新增员工.
   *
   * @param employeeDto 员工DTO
   * @return 结果
   */
  @Override
  public Result<String> save(EmployeeDto employeeDto) {

    // 2. 转换DTO为Entity
    Employee employee = EmployeeConverter.INSTANCE.d2e(employeeDto);

    // 3. 设置默认值
    employee.setStatus(StatusConstant.ENABLE);
    // 新员工使用BCrypt加密密码
    employee.setPassword(PasswordUtil.encode(PasswordConstant.DEFAULT_PASSWORD));
    employee.setCreateTime(LocalDateTime.now());
    employee.setUpdateTime(LocalDateTime.now());

    // 通过jwt令牌进行获取
    // 通过threadlocal传参
    employee.setCreateUser(BaseContext.getCurrentId());
    employee.setUpdateUser(BaseContext.getCurrentId());

    // 4. 保存到数据库
    boolean saved = this.save(employee);
    if (!saved) {
      log.error("保存员工失败：{}", employeeDto);
      throw new BaseException("新增员工失败");
    }

    log.info("新增员工成功，员工ID：{}", employee.getId());
    return Result.success();
  }

  /**
   * 分页查询员工.
   *
   * @param employeePageQueryDto 员工分页查询DTO
   * @return 分页结果
   */
  @Override
  public PageResult pageQuery(EmployeePageQueryDto employeePageQueryDto) {
    // 1. 构建分页对象
    // 从前端传来的 DTO 中提取页码 (page) 和每页条数 (pageSize)
    int page = employeePageQueryDto.getPage();
    int pageSize = employeePageQueryDto.getPageSize();
    // 2. 创建分页对象
    // 这里 new 出来的 Page 对象是 MP 的核心。
    // 此时它只是一个空壳，里面只有 page=1, size=10，但 records 是空的，total 是 0。
    Page<Employee> pageInfo = new Page<>(page, pageSize);

    // 3. 构建查询条件并执行 (关键步骤)
    lambdaQuery() // 开启链式查询，底层创建了 LambdaQueryChainWrapper
        // .like 动态拼接 SQL: WHERE name LIKE '%xxx%'
        // 第一个参数是 boolean：如果 name 不为空，才拼接这个 SQL 条件
        .like(StringUtils.hasText(employeePageQueryDto.getName()),
            Employee::getName, employeePageQueryDto.getName())

        // .orderByDesc 拼接 SQL: ORDER BY create_time DESC
        .orderByDesc(Employee::getCreateTime)

        // .page(pageInfo) 这一步发生了两件事：
        // A. 自动发起 SELECT count(*) 查询总数，填入 pageInfo.total
        // B. 自动发起 SELECT * ... LIMIT x,y 查询数据，填入 pageInfo.records
        // 注意：这里是“引用传递”，pageInfo 对象的内容被直接修改了！
        .page(pageInfo);

    // 4. 封装返回结果
    // 从已经被填充好数据的 pageInfo 中取出 total 和 records
    log.info("总共条数: {}", pageInfo.getTotal());
    log.info("当前页面数据: {}", pageInfo.getRecords());
    log.info("当前页面数据: {}",
        EmployeeConverter.INSTANCE.entityListToVoList(pageInfo.getRecords()));
    return new PageResult(pageInfo.getTotal(),
        EmployeeConverter.INSTANCE.entityListToVoList(pageInfo.getRecords()));
  }

  /**
   * 启用禁用员工账号.
   *
   * <p>功能说明：根据员工ID更新员工账号的启用/禁用状态 status = 1 表示启用，status = 0 表示禁用
   *
   * <p>实现方式：使用 MyBatis Plus 的 lambdaUpdate() 方法进行链式更新操作 该方法会自动生成 SQL: UPDATE employee SET status =
   * ?, update_time = ?, update_user = ? WHERE id = ?
   *
   * @param status     员工状态，1-启用，0-禁用（参考 StatusConstant.ENABLE 和 StatusConstant.DISABLE）
   * @param employeeId 员工ID，用于定位要更新的员工记录
   */
  @Override
  public void enableOrDisable(Integer status, Long employeeId) {
    // 使用 MyBatis Plus 的 lambdaUpdate() 方法进行链式更新
    // lambdaUpdate() 返回 LambdaUpdateChainWrapper 对象，支持链式调用
    boolean updated = lambdaUpdate()
        // .eq() 方法：设置 WHERE 条件，等同于 SQL 中的 WHERE id = ?
        // Employee::getId 是方法引用，用于指定要比较的字段（id字段）
        // employeeId 是参数值，用于匹配条件
        .eq(Employee::getId, employeeId)

        // .set() 方法：设置要更新的字段值
        // Employee::getStatus 指定要更新的字段（status字段）
        // status 是新的状态值（1-启用 或 0-禁用）
        .set(Employee::getStatus, status)

        // 同时更新修改时间，保持数据一致性
        // LocalDateTime.now() 获取当前时间作为更新时间
        .set(Employee::getUpdateTime, LocalDateTime.now())

        // 同时更新修改人，记录是谁执行了这次操作
        // BaseContext.getCurrentId() 从 ThreadLocal 中获取当前登录用户的ID
        // 这是通过 JWT 令牌解析后存储在 ThreadLocal 中的
        .set(Employee::getUpdateUser, BaseContext.getCurrentId())

        // .update() 方法：执行更新操作
        // 返回 boolean 值：true 表示更新成功，false 表示更新失败（通常是因为没有匹配的记录）
        .update();

    // 检查更新结果，如果更新失败则记录日志并抛出异常
    // 这种情况通常发生在 employeeId 不存在时
    if (!updated) {
      log.error("更新员工状态失败，员工ID：{}，目标状态：{}", employeeId, status);
      throw new AccountNotFoundException("更新员工状态失败，员工ID不存在或已被删除");
    }

    // 更新成功，记录日志
    String statusText = status.equals(StatusConstant.ENABLE) ? "启用" : "禁用";
    log.info("员工账号状态更新成功，员工ID：{}，操作：{}", employeeId, statusText);
  }

  /**
   * 更新员工信息.
   *
   * @param employeeDto 员工DTO
   */
  @Override
  public void update(EmployeeDto employeeDto) {
    // 1. 对象转换 (DTO -> Entity)
    // 使用 MapStruct，一行代码搞定，属性自动拷贝
    Employee employee = EmployeeConverter.INSTANCE.d2e(employeeDto);

    // 2. (可选) 手动设置修改时间和修改人
    // ⚠️ 注意：这种写法是"初级写法"，虽然能用，但每个 update 方法都要写一遍，很繁琐。
    // 实际项目中，这些字段通常通过 MyBatis Plus 的自动填充功能（AutoFillMetaObjectHandler）自动设置

    // 3. 调用 MP 的更新方法
    // updateById 会根据实体中的 ID 去更新其他非空字段
    updateById(employee);
  }

  /**
   * 修改密码.
   *
   * @param passwordEditDto 密码修改DTO
   */
  @Override
  public void editPassword(PasswordEditDto passwordEditDto) {
    Long empId = passwordEditDto.getEmpId();
    String oldPassword = passwordEditDto.getOldPassword();

    log.info("员工修改密码，员工ID：{}", empId);

    // 1. 根据员工ID查询员工信息
    Employee employee = this.getById(empId);
    if (employee == null) {
      log.error("修改密码失败，员工不存在，员工ID：{}", empId);
      throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
    }

    // 2. 验证旧密码是否正确
    // 使用PasswordUtil支持BCrypt和MD5两种格式，自动识别
    if (!PasswordUtil.matches(oldPassword, employee.getPassword())) {
      log.error("修改密码失败，旧密码错误，员工ID：{}", empId);
      throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
    }

    // 3. 使用BCrypt加密新密码
    String encodedNewPassword = PasswordUtil.encode(passwordEditDto.getNewPassword());

    // 4. 更新密码
    boolean updated = lambdaUpdate()
        .eq(Employee::getId, empId)
        .set(Employee::getPassword, encodedNewPassword)
        .set(Employee::getUpdateTime, LocalDateTime.now())
        .set(Employee::getUpdateUser, empId)  // 自己更新自己的密码
        .update();

    if (!updated) {
      log.error("修改密码失败，更新数据库失败，员工ID：{}", empId);
      throw new PasswordEditFailedException(MessageConstant.PASSWORD_EDIT_FAILED);
    }

    log.info("员工密码修改成功，员工ID：{}", empId);
  }
}
