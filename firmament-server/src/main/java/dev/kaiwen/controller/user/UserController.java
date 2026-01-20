package dev.kaiwen.controller.user;


import dev.kaiwen.constant.JwtClaimsConstant;
import dev.kaiwen.dto.UserLoginDto;
import dev.kaiwen.dto.UserPhoneLoginDto;
import dev.kaiwen.entity.User;
import dev.kaiwen.properties.JwtProperties;
import dev.kaiwen.result.Result;
import dev.kaiwen.service.UserService;
import dev.kaiwen.utils.JwtUtil;
import dev.kaiwen.vo.UserInfoVo;
import dev.kaiwen.vo.UserLoginVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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

    /**
     * WeChat login.
     *
     * @param userLoginDto The user login data transfer object containing WeChat authorization code.
     * @return The login result containing user information and access token.
     */
    @PostMapping("/login")
    @Operation(summary = "微信登录")
    public Result<UserLoginVo> login(@RequestBody UserLoginDto userLoginDto) {
        log.info("微信登录 {}", userLoginDto.getCode());

        User user = userService.wxLogin(userLoginDto);
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());

        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), claims);
        UserLoginVo userLoginVO = UserLoginVo.builder()
                .id(user.getId())
                .openid(user.getOpenid())
                .token(token)
                .build();

        return Result.success(userLoginVO);

    }

    /**
     * Phone number and password login.
     *
     * @param userPhoneLoginDto The user phone login data transfer object containing phone number and password.
     * @return The login result containing user information and access token.
     */
    @PostMapping("/phoneLogin")
    @Operation(summary = "手机号密码登录")
    public Result<UserLoginVo> phoneLogin(@RequestBody UserPhoneLoginDto userPhoneLoginDto) {
        log.info("手机号密码登录 {}", userPhoneLoginDto.getPhone());

        User user = userService.phoneLogin(userPhoneLoginDto);
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());

        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), claims);
        UserLoginVo userLoginVO = UserLoginVo.builder()
                .id(user.getId())
                .openid(user.getOpenid())
                .token(token)
                .build();

        return Result.success(userLoginVO);
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
        UserInfoVo userInfoVO = userService.getUserInfo();
        return Result.success(userInfoVO);
    }
}
