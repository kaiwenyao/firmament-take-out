package dev.kaiwen.controller.user;


import dev.kaiwen.constant.JwtClaimsConstant;
import dev.kaiwen.dto.UserLoginDto;
import dev.kaiwen.dto.UserPhoneLoginDto;
import dev.kaiwen.entity.User;
import dev.kaiwen.properties.JwtProperties;
import dev.kaiwen.result.Result;
import dev.kaiwen.service.UserService;
import dev.kaiwen.utils.JwtUtil;
import dev.kaiwen.vo.UserInfoVO;
import dev.kaiwen.vo.UserLoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user/user")
@Tag(name = "用户C端接口")
@Slf4j
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtProperties jwtProperties;
    @PostMapping("/login")
    @Operation(summary = "微信登录")
    public Result<UserLoginVO> login(@RequestBody UserLoginDto userLoginDTO) {
        log.info("微信登录 {}", userLoginDTO.getCode());

        User user = userService.wxLogin(userLoginDTO);
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());

        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), claims);
        UserLoginVO userLoginVO = UserLoginVO.builder()
                .id(user.getId())
                .openid(user.getOpenid())
                .token(token)
                .build();

        return Result.success(userLoginVO);

    }

    @PostMapping("/phoneLogin")
    @Operation(summary = "手机号密码登录")
    public Result<UserLoginVO> phoneLogin(@RequestBody UserPhoneLoginDto userPhoneLoginDTO) {
        log.info("手机号密码登录 {}", userPhoneLoginDTO.getPhone());

        User user = userService.phoneLogin(userPhoneLoginDTO);
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());

        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), claims);
        UserLoginVO userLoginVO = UserLoginVO.builder()
                .id(user.getId())
                .openid(user.getOpenid())
                .token(token)
                .build();

        return Result.success(userLoginVO);
    }

    @GetMapping("/info")
    @Operation(summary = "获取当前登录用户信息")
    public Result<UserInfoVO> getUserInfo() {
        log.info("获取当前登录用户信息");
        UserInfoVO userInfoVO = userService.getUserInfo();
        return Result.success(userInfoVO);
    }
}
