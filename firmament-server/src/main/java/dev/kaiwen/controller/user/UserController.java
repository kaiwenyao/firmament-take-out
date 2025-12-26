package dev.kaiwen.controller.user;


import dev.kaiwen.constant.JwtClaimsConstant;
import dev.kaiwen.dto.UserLoginDTO;
import dev.kaiwen.entity.User;
import dev.kaiwen.properties.JwtProperties;
import dev.kaiwen.result.Result;
import dev.kaiwen.service.UserService;
import dev.kaiwen.utils.JwtUtil;
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
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO) {
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
}
