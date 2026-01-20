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
import dev.kaiwen.vo.EmployeeLoginVo;
import dev.kaiwen.vo.RefreshTokenVo;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Employee management controller.
 */
@RestController
@RequestMapping("/admin/employee")
@Slf4j
@Tag(name = "员工相关接口", description = "真的是员工相关接口")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final JwtProperties jwtProperties;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Employee login.
     *
     * @param employeeLoginDto The employee login data transfer object containing username and password.
     * @return The login result containing employee information and access token.
     */
    @PostMapping("/login")
    @Operation(summary = "员工登录")
    public Result<EmployeeLoginVo> login(@RequestBody EmployeeLoginDto employeeLoginDto) {
        log.info("员工登录：{}", employeeLoginDto);

        Employee employee = employeeService.login(employeeLoginDto);

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

        EmployeeLoginVo employeeLoginVO = EmployeeLoginVo.builder()
                .id(employee.getId())
                .userName(employee.getUsername())
                .name(employee.getName())
                .token(accessToken)
                .refreshToken(refreshToken)  // 返回refresh token给前端
                .build();

        return Result.success(employeeLoginVO);
    }

    /**
     * Employee logout.
     *
     * Function description:
     * Clear the Refresh Token stored in Redis to make it invalid immediately.
     *
     * Note:
     * Since Access Token is stateless JWT, the backend cannot actively invalidate it.
     * Access Token will automatically expire after the expiration time (2 hours).
     * For stronger security, a Token blacklist mechanism can be implemented.
     *
     * @return The logout result, returns success message on success.
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
     * Refresh Access Token.
     *
     * Function description:
     * When Access Token is about to expire or has expired, the frontend uses Refresh Token to get a new Access Token.
     *
     * Security mechanism:
     * 1. Verify the signature and validity period of Refresh Token.
     * 2. Get the stored Refresh Token from Redis for secondary verification to prevent forgery.
     * 3. Generate a new Access Token, Refresh Token remains unchanged.
     *
     * @param refreshTokenDto The request object containing refresh token.
     * @return The new Access Token and original Refresh Token.
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新Access Token")
    public Result<RefreshTokenVo> refreshToken(@RequestBody RefreshTokenDto refreshTokenDto) {
        try {
            String refreshToken = refreshTokenDto.getRefreshToken();

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
            RefreshTokenVo refreshTokenVO = RefreshTokenVo.builder()
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
     * Create a new employee.
     *
     * @param employeeDto The employee data transfer object containing employee information.
     * @return The operation result, returns success message on success.
     */
    @PostMapping
    @Operation(summary = "新增员工")
    public Result<String> save(@RequestBody EmployeeDto employeeDto) {
        log.info("收到新增员工请求：{}", employeeDto);
        return employeeService.save(employeeDto);
    }

    /**
     * Page query for employees.
     *
     * @param employeePageQueryDto The employee page query conditions, including page number, page size,
     *                             employee name, username and other query parameters.
     * @return The page query result containing employee list and pagination information.
     */
    @GetMapping("/page")
    @Operation(summary = "员工分页查询")
    public Result<PageResult> page(EmployeePageQueryDto employeePageQueryDto) {
        log.info("员工分页查询，参数：{}", employeePageQueryDto);
        PageResult pageResult = employeeService.pageQuery(employeePageQueryDto);
        return Result.success(pageResult);
    }

    /**
     * Enable or disable employee account.
     *
     * @param status     The employee status, 1 means enabled, 0 means disabled.
     * @param employeeId The employee ID.
     * @return The operation result, returns success message on success.
     */
    @PostMapping("/status/{status}")
    @Operation(summary = "启用禁用员工账号")
    public Result enableOrDisableEmployee(@PathVariable Integer status, @RequestParam(value = "id") Long employeeId) {
        log.info("启用禁用员工账号:{},{}", status, employeeId);
        employeeService.enableOrDisable(status, employeeId);
        return Result.success();
    }

    /**
     * Get employee by ID.
     *
     * @param id The employee ID.
     * @return The employee information.
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据id查询员工信息")
    public Result<Employee> getById(@PathVariable Long id) {
        Employee employee = employeeService.getById(id);
        employee.setPassword("****");
        return Result.success(employee);
    }

    /**
     * Update employee information.
     *
     * @param employeeDto The employee data transfer object containing employee ID and updated information.
     * @return The operation result, returns success message on success.
     */
    @PutMapping
    @Operation(summary = "编辑员工信息")
    public Result update(@RequestBody EmployeeDto employeeDto) {
        log.info("编辑员工信息: {}", employeeDto);
        employeeService.update(employeeDto);
        return Result.success();
    }

    /**
     * Edit password.
     *
     * @param passwordEditDto The password edit data transfer object containing old password and new password.
     * @return The operation result, returns success message on success.
     */
    @PutMapping("/editPassword")
    @Operation(summary = "修改密码")
    public Result<String> editPassword(@RequestBody PasswordEditDto passwordEditDto) {
        log.info("修改密码: {}", passwordEditDto);
        employeeService.editPassword(passwordEditDto);
        return Result.success();
    }
}
