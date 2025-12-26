package dev.kaiwen.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kaiwen.constant.MessageConstant;
import dev.kaiwen.dto.UserLoginDTO;
import dev.kaiwen.entity.User;
import dev.kaiwen.exception.LoginFailedException;
import dev.kaiwen.mapper.UserMapper;
import dev.kaiwen.properties.WeChatProperties;
import dev.kaiwen.service.UserService;
import dev.kaiwen.utils.HttpClientUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    public static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";
    private final WeChatProperties weChatProperties;
    private final ObjectMapper objectMapper;

    @Override
    public User wxLogin(UserLoginDTO userLoginDTO) {
        String openid = getOpenId(userLoginDTO.getCode());
        // 判断open id是否为空
        if (openid == null) {
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);

        }
//        User user = this.getById(openid);
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
        String openid = null;
        try {
            JsonNode jsonNode = objectMapper.readTree(s);
            openid = jsonNode.get("openid").asText();
        } catch (JsonProcessingException e)
        {
            throw new RuntimeException("微信登录解析失败", e);
        }
        return openid;
    }
}
