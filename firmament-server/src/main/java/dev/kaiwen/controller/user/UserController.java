package dev.kaiwen.controller.user;

import dev.kaiwen.constant.JwtClaimsConstant;
import dev.kaiwen.dto.UserLoginDto;
import dev.kaiwen.dto.UserPhoneLoginDto;
import dev.kaiwen.entity.User;
import dev.kaiwen.properties.JwtProperties;
import dev.kaiwen.result.Result;
import dev.kaiwen.service.UserService;
import dev.kaiwen.utils.JwtService;
import dev.kaiwen.vo.UserInfoVo;
import dev.kaiwen.vo.UserLoginVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * User controller for client side.
 */
@RestController
@RequestMapping("/user/user")
@Tag(name = "用户C端接口")
@Slf4j
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;
  private final JwtProperties jwtProperties;
  private final JwtService jwtService;

  /**
   * WeChat login.
   *
   * @param userLoginDto The user login data transfer object containing WeChat authorisation code.
   * @return The login result containing user information and access token.
   */
  @PostMapping("/login")
  @Operation(summary = "微信登录")
  public Result<UserLoginVo> login(@RequestBody UserLoginDto userLoginDto) {
    log.info("微信登录 {}", userLoginDto.getCode());

    User user = userService.wxLogin(userLoginDto);
    UserLoginVo userLoginVo = buildUserLoginVo(user);

    return Result.success(userLoginVo);
  }

  /**
   * Phone number and password login.
   *
   * @param userPhoneLoginDto The user phone login data transfer object containing phone number and
   *                          password.
   * @return The login result containing user information and access token.
   */
  @PostMapping("/phoneLogin")
  @Operation(summary = "手机号密码登录")
  public Result<UserLoginVo> phoneLogin(@RequestBody UserPhoneLoginDto userPhoneLoginDto) {
    log.info("手机号密码登录 {}", userPhoneLoginDto.getPhone());

    User user = userService.phoneLogin(userPhoneLoginDto);
    UserLoginVo userLoginVo = buildUserLoginVo(user);

    return Result.success(userLoginVo);
  }

  /**
   * Get current logged-in user information.
   *
   * @return The current user information.
   */
  @GetMapping("/info")
  @Operation(summary = "获取当前登录用户信息")
  public Result<UserInfoVo> getUserInfo() {
    log.info("获取当前登录用户信息");
    UserInfoVo userInfoVo = userService.getUserInfo();
    return Result.success(userInfoVo);
  }

  /**
   * Build UserLoginVo from User entity.
   *
   * @param user The user entity.
   * @return The UserLoginVo containing user information and JWT token.
   */
  private UserLoginVo buildUserLoginVo(User user) {
    Map<String, Object> claims = new HashMap<>();
    claims.put(JwtClaimsConstant.USER_ID, user.getId());

    String token = jwtService.createJwt(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(),
        claims);
    return UserLoginVo.builder()
        .id(user.getId())
        .openid(user.getOpenid())
        .token(token)
        .build();
  }
}
