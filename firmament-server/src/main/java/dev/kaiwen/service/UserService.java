package dev.kaiwen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import dev.kaiwen.dto.UserLoginDto;
import dev.kaiwen.dto.UserPhoneLoginDto;
import dev.kaiwen.entity.User;
import dev.kaiwen.vo.UserInfoVo;

/**
 * 用户服务接口.
 */
public interface UserService extends IService<User> {

  /**
   * 微信登录.
   *
   * @param userLoginDto 用户登录DTO
   * @return 用户信息
   */
  User wxLogin(UserLoginDto userLoginDto);

  /**
   * 用户手机号密码登录.
   *
   * @param userPhoneLoginDto 手机号和密码
   * @return 用户信息
   */
  User phoneLogin(UserPhoneLoginDto userPhoneLoginDto);

  /**
   * 获取当前登录用户信息.
   *
   * @return 用户信息
   */
  UserInfoVo getUserInfo();
}

