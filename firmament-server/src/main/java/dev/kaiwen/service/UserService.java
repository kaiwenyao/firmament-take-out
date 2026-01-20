package dev.kaiwen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import dev.kaiwen.dto.UserLoginDto;
import dev.kaiwen.dto.UserPhoneLoginDto;
import dev.kaiwen.entity.User;
import dev.kaiwen.vo.UserInfoVO;

public interface UserService extends IService<User> {


    User wxLogin(UserLoginDto userLoginDTO);

    /**
     * 用户手机号密码登录
     * @param userPhoneLoginDTO 手机号和密码
     * @return 用户信息
     */
    User phoneLogin(UserPhoneLoginDto userPhoneLoginDTO);

    /**
     * 获取当前登录用户信息
     * @return 用户信息
     */
    UserInfoVO getUserInfo();
}

