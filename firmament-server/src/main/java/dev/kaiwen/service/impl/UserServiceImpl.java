package dev.kaiwen.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kaiwen.constant.MessageConstant;
import dev.kaiwen.context.BaseContext;
import dev.kaiwen.dto.UserLoginDto;
import dev.kaiwen.dto.UserPhoneLoginDto;
import dev.kaiwen.entity.User;
import dev.kaiwen.exception.AccountNotFoundException;
import dev.kaiwen.exception.LoginFailedException;
import dev.kaiwen.exception.PasswordErrorException;
import dev.kaiwen.mapper.UserMapper;
import dev.kaiwen.properties.WeChatProperties;
import dev.kaiwen.service.UserService;
import dev.kaiwen.utils.HttpClientUtil;
import dev.kaiwen.utils.PasswordUtil;
import dev.kaiwen.vo.UserInfoVo;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现类.
 * 提供微信登录、手机号密码登录、用户信息查询等功能.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

  public static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";
  private final WeChatProperties weChatProperties;
  private final ObjectMapper objectMapper;

  /**
   * 微信登录.
   *
   * @param userLoginDto 用户登录DTO
   * @return 用户信息
   */
  @Override
  public User wxLogin(UserLoginDto userLoginDto) {
    String openid = getOpenId(userLoginDto.getCode());
    // 判断open id是否为空
    if (openid == null) {
      throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
    }
    User user = lambdaQuery().eq(User::getOpenid, openid).one();
    if (user == null) {
      user = User.builder()
          .openid(openid)
          .createTime(LocalDateTime.now())
          .build();
      this.save(user);
    }
    return user;

  }

  private String getOpenId(String code) {
    Map<String, String> params = new HashMap<>();
    params.put("appid", weChatProperties.getAppid());
    params.put("secret", weChatProperties.getSecret());
    params.put("js_code", code);
    params.put("grant_type", "authorization_code");

    // 调用微信服务器接口 获取open id
    String s = HttpClientUtil.doGet(WX_LOGIN, params);
    String openid;
    try {
      JsonNode jsonNode = objectMapper.readTree(s);
      openid = jsonNode.get("openid").asText();
    } catch (JsonProcessingException e) {
      log.error("微信登录解析失败", e);
      throw new LoginFailedException("微信登录解析失败");
    }
    return openid;
  }

  /**
   * 用户手机号密码登录.
   *
   * @param userPhoneLoginDto 手机号和密码
   * @return 用户信息
   */
  @Override
  public User phoneLogin(UserPhoneLoginDto userPhoneLoginDto) {
    String phone = userPhoneLoginDto.getPhone();
    String password = userPhoneLoginDto.getPassword();

    // 1、根据手机号查询数据库中的数据
    User user = lambdaQuery()
        .eq(User::getPhone, phone)
        .one();

    // 2、处理各种异常情况（手机号不存在、密码不对）
    if (user == null) {
      // 账号不存在
      throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
    }

    // 3、密码比对
    // 使用PasswordUtil支持BCrypt和MD5两种格式，自动识别
    if (user.getPassword() == null || PasswordUtil.mismatches(password, user.getPassword())) {
      // 密码错误
      throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
    }

    // ⭐ 安全优化：如果密码是MD5格式，自动升级为BCrypt
    if (PasswordUtil.isMd5(user.getPassword())) {
      log.info("检测到用户 {} 使用MD5密码，正在自动升级为BCrypt加密", phone);
      // 使用BCrypt重新加密密码
      String bcryptPassword = PasswordUtil.encode(password);
      user.setPassword(bcryptPassword);
      // 更新数据库中的密码
      this.updateById(user);
      log.info("用户 {} 的密码已成功升级为BCrypt加密格式", phone);
    }

    return user;
  }

  @Override
  public UserInfoVo getUserInfo() {
    // 从ThreadLocal中获取当前登录用户ID
    Long userId = BaseContext.getCurrentId();
    log.info("获取当前用户信息，用户ID：{}", userId);

    // 根据用户ID查询用户信息
    User user = this.getById(userId);
    if (user == null) {
      throw new AccountNotFoundException("用户不存在");
    }

    // 转换为VO对象
    return UserInfoVo.builder()
        .id(user.getId())
        .phone(user.getPhone())
        .name(user.getName())
        .avatar(user.getAvatar())
        .idNumber(user.getIdNumber())
        .build();
  }
}
