package dev.kaiwen.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kaiwen.constant.JwtClaimsConstant;
import dev.kaiwen.constant.MessageConstant;
import dev.kaiwen.context.BaseContext;
import dev.kaiwen.properties.JwtProperties;
import dev.kaiwen.result.Result;
import dev.kaiwen.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.PrintWriter;

/**
 * jwt令牌校验的拦截器
 */
@Component
@Slf4j
public class JwtTokenUserInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 校验jwt
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 判断当前拦截到的是Controller的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            // 当前拦截到的不是动态方法，直接放行
            return true;
        }

        // 1、从请求头中获取令牌
        String token = request.getHeader(jwtProperties.getUserTokenName());

        // 2、校验令牌
        try {
            // 检查token是否为空
            if (token == null || token.trim().isEmpty()) {
                log.warn("JWT token为空");
                BaseContext.removeCurrentId();
                writeErrorResponse(response, MessageConstant.USER_NOT_LOGIN);
                return false;
            }

            // 只记录token的前10位，避免完整token泄露到日志
            log.info("jwt校验: {}...", token.substring(0, Math.min(10, token.length())));
            Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
            Long userId = Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());
            log.info("当前用户id：{}", userId);
            BaseContext.setCurrentId(userId);
            // 3、通过，放行
            return true;
        } catch (Exception ex) {
            // 验证失败时也要清理（虽然这里可能没有设置，但为了安全）
            BaseContext.removeCurrentId();
            // 4、不通过，响应401状态码
            log.warn("JWT token验证失败: {}", ex.getMessage());
            writeErrorResponse(response, MessageConstant.USER_NOT_LOGIN);
            return false;
        }
    }

    /**
     * 请求处理完成后清理 ThreadLocal，防止内存泄漏和数据污染
     *
     * @param request
     * @param response
     * @param handler
     * @param ex
     * @throws Exception
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        // 清理 ThreadLocal，防止线程复用导致的数据污染和内存泄漏
        BaseContext.removeCurrentId();
    }

    /**
     * 写入错误响应（401状态码 + JSON格式）
     */
    private void writeErrorResponse(HttpServletResponse response, String message) throws Exception {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        Result<Object> result = Result.error(message);
        String json = objectMapper.writeValueAsString(result);
        PrintWriter writer = response.getWriter();
        writer.write(json);
        writer.flush();
    }
}
