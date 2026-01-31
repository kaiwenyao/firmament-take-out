package dev.kaiwen.controller.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kaiwen.constant.JwtClaimsConstant;
import dev.kaiwen.dto.UserLoginDto;
import dev.kaiwen.dto.UserPhoneLoginDto;
import dev.kaiwen.entity.User;
import dev.kaiwen.properties.JwtProperties;
import dev.kaiwen.service.UserService;
import dev.kaiwen.utils.JwtService;
import dev.kaiwen.vo.UserInfoVo;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
class UserControllerTest {

  @MockitoBean
  private UserService userService;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private JwtProperties jwtProperties;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private MockMvc mockMvc;

  private void setupUserJwtMock() {
    given(jwtProperties.getUserTokenName()).willReturn("token");
    given(jwtProperties.getUserSecretKey()).willReturn("mock-secret-key");
    given(jwtProperties.getUserTtl()).willReturn(7200000L);
    Claims claims = org.mockito.Mockito.mock(Claims.class);
    given(jwtService.parseJwt(eq("mock-secret-key"),
        org.mockito.ArgumentMatchers.anyString())).willReturn(claims);
    given(claims.get(JwtClaimsConstant.USER_ID)).willReturn("1");
  }

  @Test
  void loginSuccess() throws Exception {
    UserLoginDto dto = new UserLoginDto();
    dto.setCode("wx_code_123");

    User user = User.builder().id(1L).openid("openid_abc").build();
    given(userService.wxLogin(any(UserLoginDto.class))).willReturn(user);
    given(jwtProperties.getUserSecretKey()).willReturn("mock-secret-key");
    given(jwtProperties.getUserTtl()).willReturn(7200000L);
    given(jwtService.createJwt(eq("mock-secret-key"), eq(7200000L),
        org.mockito.ArgumentMatchers.any())).willReturn("mock-token");

    mockMvc.perform(post("/user/user/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data.id").value(1))
        .andExpect(jsonPath("$.data.openid").value("openid_abc"))
        .andExpect(jsonPath("$.data.token").value("mock-token"));

    verify(userService).wxLogin(any(UserLoginDto.class));
  }

  @Test
  void phoneLoginSuccess() throws Exception {
    UserPhoneLoginDto dto = new UserPhoneLoginDto();
    dto.setPhone("13800138000");
    dto.setPassword("123456");

    User user = User.builder().id(1L).openid("").phone("13800138000").build();
    given(userService.phoneLogin(any(UserPhoneLoginDto.class))).willReturn(user);
    given(jwtProperties.getUserSecretKey()).willReturn("mock-secret-key");
    given(jwtProperties.getUserTtl()).willReturn(7200000L);
    given(jwtService.createJwt(eq("mock-secret-key"), eq(7200000L),
        org.mockito.ArgumentMatchers.any())).willReturn("mock-token");

    mockMvc.perform(post("/user/user/phoneLogin")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data.id").value(1))
        .andExpect(jsonPath("$.data.token").value("mock-token"));

    verify(userService).phoneLogin(any(UserPhoneLoginDto.class));
  }

  @Test
  void getUserInfoSuccess() throws Exception {
    setupUserJwtMock();

    UserInfoVo vo = UserInfoVo.builder()
        .id(1L)
        .phone("13800138000")
        .name("测试用户")
        .avatar("http://avatar.jpg")
        .build();
    given(userService.getUserInfo()).willReturn(vo);

    mockMvc.perform(get("/user/user/info").header("token", "mock-accessToken"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data.id").value(1))
        .andExpect(jsonPath("$.data.phone").value("13800138000"))
        .andExpect(jsonPath("$.data.name").value("测试用户"));

    verify(userService).getUserInfo();
  }
}
