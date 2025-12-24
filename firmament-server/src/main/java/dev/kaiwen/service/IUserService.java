package dev.kaiwen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import dev.kaiwen.dto.UserLoginDTO;
import dev.kaiwen.entity.User;
import dev.kaiwen.vo.UserLoginVO;

public interface IUserService extends IService<User> {


    User wxLogin(UserLoginDTO userLoginDTO);
}
