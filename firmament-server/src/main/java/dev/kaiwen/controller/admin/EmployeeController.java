package dev.kaiwen.controller.admin;

import dev.kaiwen.constant.JwtClaimsConstant;
import dev.kaiwen.dto.EmployeeDto;
import dev.kaiwen.dto.EmployeeLoginDto;
import dev.kaiwen.dto.EmployeePageQueryDto;
import dev.kaiwen.dto.PasswordEditDto;
import dev.kaiwen.dto.RefreshTokenDto;
import dev.kaiwen.entity.Employee;
import dev.kaiwen.properties.JwtProperties;
import dev.kaiwen.result.PageResult;
import dev.kaiwen.result.Result;
import dev.kaiwen.service.EmployeeService;
import dev.kaiwen.utils.JwtUtil;
import dev.kaiwen.vo.EmployeeLoginVO;
import dev.kaiwen.vo.RefreshTokenVO;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 员工管理
 */
@RestController
@RequestMapping("/admin/employee")
@Slf4j
@Tag(name = "员工相关接口", description = "真的是员工相关接口")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private JwtProperties jwtProperties;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 登录
     *
     * @param employeeLoginDTO
     * @return
     */
    @PostMapping("/login")
    @Operation(summary = "员工登录")
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDto employeeLoginDTO) {
        log.info("员工登录：{}", employeeLoginDTO);

        Employee employee = employeeService.login(employeeLoginDTO);

        // 登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, employee.getId());

        // 生成 Access Token（2小时有效）
        String accessToken = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

        // ⭐ 生成 Refresh Token（7天有效）
        String refreshToken = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminRefreshTtl(),
                claims);

        // 将 Refresh Token 存入 Redis，key为 "refresh_token:{empId}"，过期时间7天
        String redisKey = "refresh_token:" + employee.getId();
        redisTemplate.opsForValue().set(
                redisKey,
                refreshToken,
                jwtProperties.getAdminRefreshTtl(),
                TimeUnit.MILLISECONDS
        );

        log.info("员工 {} 登录成功，Refresh Token已存入Redis，key={}", employee.getUsername(), redisKey);

        EmployeeLoginVO employeeLoginVO = EmployeeLoginVO.builder()
                .id(employee.getId())
                .userName(employee.getUsername())
                .name(employee.getName())
                .token(accessToken)
                .refreshToken(refreshToken)  // 返回refresh token给前端
                .build();

        return Result.success(employeeLoginVO);
    }

    /**
     * 退出登录
     *
     * 功能说明：
     * 清除Redis中存储的Refresh Token，使其立即失效
     *
     * 注意：
     * 由于Access Token是无状态的JWT，后端无法主动使其失效
     * Access Token会在过期时间到期后自动失效（2小时）
     * 如需要更强的安全性，可以实现Token黑名单机制
     *
     * @return 退出结果
     */
    @Operation(summary = "员工退出登录")
    @PostMapping("/logout")
    public Result<String> logout() {
        try {
            // 从ThreadLocal获取当前登录的员工ID
            Long empId = dev.kaiwen.context.BaseContext.getCurrentId();

            if (empId != null) {
                // 清除Redis中的Refresh Token
                String redisKey = "refresh_token:" + empId;
                Boolean deleted = redisTemplate.delete(redisKey);

                if (Boolean.TRUE.equals(deleted)) {
                    log.info("员工 {} 已退出登录，Refresh Token已从Redis清除", empId);
                } else {
                    log.warn("员工 {} 退出登录，但Redis中未找到Refresh Token", empId);
                }
            }

            return Result.success();
        } catch (Exception e) {
            log.error("退出登录时发生错误：{}", e.getMessage(), e);
            // 即使发生错误，也返回成功，让前端清除本地token
            return Result.success();
        }
    }

    /**
     * ⭐ 刷新Token
     *
     * 功能说明：
     * 当Access Token即将过期或已过期时，前端使用Refresh Token获取新的Access Token
     *
     * 安全机制：
     * 1. 验证Refresh Token的签名和有效期
     * 2. 从Redis中获取存储的Refresh Token进行二次验证，防止伪造
     * 3. 生成新的Access Token，Refresh Token保持不变
     *
     * @param refreshTokenDTO 包含refresh token的请求对象
     * @return 新的Access Token和原Refresh Token
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新Access Token")
    public Result<RefreshTokenVO> refreshToken(@RequestBody RefreshTokenDto refreshTokenDTO) {
        try {
            String refreshToken = refreshTokenDTO.getRefreshToken();

            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                log.warn("刷新Token失败：Refresh Token为空");
                return Result.error("Refresh Token不能为空");
            }

            // 1. 验证并解析 Refresh Token
            Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), refreshToken);
            Long empId = Long.valueOf(claims.get(JwtClaimsConstant.EMP_ID).toString());

            log.info("收到Token刷新请求，员工ID：{}", empId);

            // 2. 从Redis获取存储的Refresh Token进行二次验证
            String redisKey = "refresh_token:" + empId;
            String storedRefreshToken = redisTemplate.opsForValue().get(redisKey);

            if (storedRefreshToken == null) {
                log.warn("刷新Token失败：Redis中未找到Refresh Token，员工ID：{}", empId);
                return Result.error("Refresh Token已失效，请重新登录");
            }

            if (!refreshToken.equals(storedRefreshToken)) {
                log.warn("刷新Token失败：Refresh Token不匹配，员工ID：{}", empId);
                return Result.error("Refresh Token无效，请重新登录");
            }

            // 3. 生成新的 Access Token
            Map<String, Object> newClaims = new HashMap<>();
            newClaims.put(JwtClaimsConstant.EMP_ID, empId);

            String newAccessToken = JwtUtil.createJWT(
                    jwtProperties.getAdminSecretKey(),
                    jwtProperties.getAdminTtl(),
                    newClaims);

            log.info("Token刷新成功，员工ID：{}，新Access Token已生成", empId);

            // 4. 返回新的Access Token和原Refresh Token
            RefreshTokenVO refreshTokenVO = RefreshTokenVO.builder()
                    .token(newAccessToken)
                    .refreshToken(refreshToken)  // Refresh Token保持不变
                    .build();

            return Result.success(refreshTokenVO);

        } catch (Exception e) {
            log.error("刷新Token失败：{}", e.getMessage(), e);
            return Result.error("Refresh Token无效或已过期，请重新登录");
        }
    }

    /**
     * 新增员工
     *
     * @param employeeDTO
     * @return
     */
    @PostMapping
    @Operation(summary = "新增员工")
    public Result<String> save(@RequestBody EmployeeDto employeeDTO) {
        log.info("收到新增员工请求：{}", employeeDTO);
        return employeeService.save(employeeDTO);
    }

    @GetMapping("/page")
    @Operation(summary = "员工分页查询")
    public Result<PageResult> page(EmployeePageQueryDto employeePageQueryDTO) {
        log.info("员工分页查询，参数：{}", employeePageQueryDTO);
        PageResult pageResult = employeeService.pageQuery(employeePageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 启用禁用员工账号
     *
     * @param status
     * @param employeeId
     * @return
     */
    @PostMapping("/status/{status}")
    @Operation(summary = "启用禁用员工账号")
    public Result enableOrDisableEmployee(@PathVariable Integer status, @RequestParam(value = "id") Long employeeId) {
        log.info("启用禁用员工账号:{},{}", status, employeeId);
        employeeService.enableOrDisable(status, employeeId);
        return Result.success();
    }

    /**
     * 根据id查询员工信息
     * @param employeeId
     * @return
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据id查询员工信息")
    public Result<Employee> getById(@PathVariable(value = "id") Long employeeId) {
        Employee employee = employeeService.getById(employeeId);
        employee.setPassword("****");
        return Result.success(employee);
    }

    /**
     * 编辑员工信息
     * @param employeeDTO
     * @return
     */
    @PutMapping
    @Operation(summary = "编辑员工信息")
    public Result update(@RequestBody EmployeeDto employeeDTO) {
        log.info("编辑员工信息: {}", employeeDTO);
        employeeService.update(employeeDTO);
        return Result.success();
    }

    /**
     * 修改密码
     * @param passwordEditDTO
     * @return
     */
    @PutMapping("/editPassword")
    @Operation(summary = "修改密码")
    public Result<String> editPassword(@RequestBody PasswordEditDto passwordEditDTO) {
        log.info("修改密码: {}", passwordEditDTO);
        employeeService.editPassword(passwordEditDTO);
        return Result.success();
    }
}
