package dev.kaiwen.service.impl;

import static dev.kaiwen.constant.MessageConstant.ACCOUNT_NOT_FOUND;
import static dev.kaiwen.constant.MessageConstant.LOGIN_FAILED;
import static dev.kaiwen.constant.MessageConstant.PASSWORD_ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import dev.kaiwen.context.BaseContext;
import dev.kaiwen.dto.UserLoginDto;
import dev.kaiwen.dto.UserPhoneLoginDto;
import dev.kaiwen.entity.User;
import dev.kaiwen.exception.AccountNotFoundException;
import dev.kaiwen.exception.LoginFailedException;
import dev.kaiwen.exception.PasswordErrorException;
import dev.kaiwen.mapper.UserMapper;
import dev.kaiwen.properties.WeChatProperties;
import dev.kaiwen.utils.HttpClientUtil;
import dev.kaiwen.utils.PasswordService;
import dev.kaiwen.vo.UserInfoVo;
import java.time.LocalDateTime;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

  @InjectMocks
  private UserServiceImpl userService;

  @Mock
  private UserMapper mapper;

  @Mock
  private WeChatProperties weChatProperties;

  @Mock
  private ObjectMapper objectMapper;

  @Mock
  private PasswordService passwordService;

  @Captor
  private ArgumentCaptor<User> userCaptor;

  @BeforeEach
  void setUp() {
    MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
    TableInfoHelper.initTableInfo(assistant, User.class);
    ReflectionTestUtils.setField(userService, "baseMapper", mapper);
  }

  private void stubWeChatProperties() {
    when(weChatProperties.getAppid()).thenReturn("appid");
    when(weChatProperties.getSecret()).thenReturn("secret");
  }

  @Test
  void wxLoginCreatesUserWhenNotExists() throws Exception {
    UserLoginDto dto = new UserLoginDto();
    dto.setCode("code");

    stubWeChatProperties();
    JsonNode root = mock(JsonNode.class);
    when(root.get("openid")).thenReturn(TextNode.valueOf("openid-1"));
    when(objectMapper.readTree(anyString())).thenReturn(root);
    when(mapper.selectOne(any())).thenReturn(null);
    when(mapper.insert(any(User.class))).thenReturn(1);

    try (MockedStatic<HttpClientUtil> httpClient = mockStatic(HttpClientUtil.class)) {
      httpClient.when(() -> HttpClientUtil.doGet(anyString(), any())).thenReturn("{}");

      User result = userService.wxLogin(dto);

      assertNotNull(result);
      assertEquals("openid-1", result.getOpenid());
      verify(mapper).insert(userCaptor.capture());
    }
  }

  @Test
  void wxLoginReturnsExistingUser() throws Exception {
    UserLoginDto dto = new UserLoginDto();
    dto.setCode("code");

    stubWeChatProperties();
    JsonNode root = mock(JsonNode.class);
    when(root.get("openid")).thenReturn(TextNode.valueOf("openid-2"));
    when(objectMapper.readTree(anyString())).thenReturn(root);

    User existing = new User();
    existing.setId(10L);
    existing.setOpenid("openid-2");
    when(mapper.selectOne(any())).thenReturn(existing);

    try (MockedStatic<HttpClientUtil> httpClient = mockStatic(HttpClientUtil.class)) {
      httpClient.when(() -> HttpClientUtil.doGet(anyString(), any())).thenReturn("{}");

      User result = userService.wxLogin(dto);

      assertEquals(10L, result.getId());
      verify(mapper, never()).insert(any(User.class));
    }
  }

  @Test
  void wxLoginOpenIdNullThrows() throws Exception {
    UserLoginDto dto = new UserLoginDto();
    dto.setCode("code");

    stubWeChatProperties();
    JsonNode root = mock(JsonNode.class);
    JsonNode openIdNode = mock(JsonNode.class);
    when(openIdNode.asText()).thenReturn(null);
    when(root.get("openid")).thenReturn(openIdNode);
    when(objectMapper.readTree(anyString())).thenReturn(root);

    try (MockedStatic<HttpClientUtil> httpClient = mockStatic(HttpClientUtil.class)) {
      httpClient.when(() -> HttpClientUtil.doGet(anyString(), any())).thenReturn("{}");

      LoginFailedException exception = assertThrows(LoginFailedException.class,
          () -> userService.wxLogin(dto));

      assertEquals(LOGIN_FAILED, exception.getMessage());
    }
  }

  @Test
  void wxLoginParseFailureThrows() throws Exception {
    UserLoginDto dto = new UserLoginDto();
    dto.setCode("code");

    stubWeChatProperties();
    when(objectMapper.readTree(anyString())).thenThrow(new JsonProcessingException("bad") {
    });

    try (MockedStatic<HttpClientUtil> httpClient = mockStatic(HttpClientUtil.class)) {
      httpClient.when(() -> HttpClientUtil.doGet(anyString(), any())).thenReturn("bad");

      LoginFailedException exception = assertThrows(LoginFailedException.class,
          () -> userService.wxLogin(dto));

      assertEquals("微信登录解析失败", exception.getMessage());
    }
  }

  @Test
  void phoneLoginAccountNotFound() {
    UserPhoneLoginDto dto = new UserPhoneLoginDto();
    dto.setPhone("123");
    dto.setPassword("pw");

    when(mapper.selectOne(any())).thenReturn(null);

    AccountNotFoundException exception = assertThrows(AccountNotFoundException.class,
        () -> userService.phoneLogin(dto));

    assertEquals(ACCOUNT_NOT_FOUND, exception.getMessage());
  }

  @Test
  void phoneLoginPasswordNullThrows() {
    UserPhoneLoginDto dto = new UserPhoneLoginDto();
    dto.setPhone("123");
    dto.setPassword("pw");

    User user = new User();
    user.setPassword(null);
    when(mapper.selectOne(any())).thenReturn(user);

    PasswordErrorException exception = assertThrows(PasswordErrorException.class,
        () -> userService.phoneLogin(dto));

    assertEquals(PASSWORD_ERROR, exception.getMessage());
    verify(passwordService, never()).mismatches(anyString(), anyString());
  }

  @Test
  void phoneLoginPasswordError() {
    UserPhoneLoginDto dto = new UserPhoneLoginDto();
    dto.setPhone("123");
    dto.setPassword("pw");

    User user = new User();
    user.setPassword("encoded");
    when(mapper.selectOne(any())).thenReturn(user);
    when(passwordService.mismatches("pw", "encoded")).thenReturn(true);

    PasswordErrorException exception = assertThrows(PasswordErrorException.class,
        () -> userService.phoneLogin(dto));

    assertEquals(PASSWORD_ERROR, exception.getMessage());
  }

  @Test
  void phoneLoginUpgradesMd5Password() {
    UserPhoneLoginDto dto = new UserPhoneLoginDto();
    dto.setPhone("123");
    dto.setPassword("pw");

    User user = new User();
    user.setPassword("md5hash");
    when(mapper.selectOne(any())).thenReturn(user);
    when(passwordService.mismatches("pw", "md5hash")).thenReturn(false);
    when(passwordService.isMd5("md5hash")).thenReturn(true);
    when(passwordService.encode("pw")).thenReturn("bcrypt");
    when(mapper.updateById(any(User.class))).thenReturn(1);

    User result = userService.phoneLogin(dto);

    assertEquals("bcrypt", result.getPassword());
    verify(mapper).updateById(userCaptor.capture());
  }

  @Test
  void phoneLoginNoUpgradeWhenNotMd5() {
    UserPhoneLoginDto dto = new UserPhoneLoginDto();
    dto.setPhone("123");
    dto.setPassword("pw");

    User user = new User();
    user.setPassword("bcrypt");
    when(mapper.selectOne(any())).thenReturn(user);
    when(passwordService.mismatches("pw", "bcrypt")).thenReturn(false);
    when(passwordService.isMd5("bcrypt")).thenReturn(false);

    userService.phoneLogin(dto);

    verify(mapper, never()).updateById(any(User.class));
  }

  @Test
  void getUserInfoSuccess() {
    User user = new User();
    user.setId(11L);
    user.setPhone("138");
    user.setName("kaiwen");
    user.setAvatar("a");
    user.setIdNumber("id");
    user.setCreateTime(LocalDateTime.now());

    when(mapper.selectById(11L)).thenReturn(user);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(11L);

      UserInfoVo result = userService.getUserInfo();

      assertEquals("kaiwen", result.getName());
      assertEquals("138", result.getPhone());
    }
  }

  @Test
  void getUserInfoNotFoundThrows() {
    when(mapper.selectById(12L)).thenReturn(null);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(12L);

      AccountNotFoundException exception = assertThrows(AccountNotFoundException.class,
          () -> userService.getUserInfo());

      assertEquals("用户不存在", exception.getMessage());
    }
  }
}
