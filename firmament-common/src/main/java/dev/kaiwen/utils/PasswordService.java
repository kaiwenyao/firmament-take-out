package dev.kaiwen.utils;

import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

/**
 * 密码服务类 支持BCrypt和MD5两种加密方式，用于逐步迁移.
 */
@Component
@Slf4j
public class PasswordService {

  /**
   * BCrypt密码编码器.
   */
  private final BCryptPasswordEncoder passwordEncoder;

  /**
   * MD5密码的前缀标识.
   */
  private static final String MD5_PREFIX = "{MD5}";
  /**
   * BCrypt密码的前缀标识.
   */
  private static final String BCRYPT_PREFIX = "{BCRYPT}";

  /**
   * 构造函数注入 BCryptPasswordEncoder.
   *
   * @param passwordEncoder BCrypt密码编码器
   */
  public PasswordService(BCryptPasswordEncoder passwordEncoder) {
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * 加密密码（使用BCrypt）.
   *
   * @param rawPassword 原始密码
   * @return 加密后的密码（带BCRYPT前缀）
   */
  public String encode(String rawPassword) {
    String encoded = passwordEncoder.encode(rawPassword);
    return BCRYPT_PREFIX + encoded;
  }

  /**
   * 验证密码是否不匹配 支持BCrypt和MD5两种格式，自动识别.
   *
   * @param rawPassword     原始密码
   * @param encodedPassword 加密后的密码（可能带前缀）
   * @return true 表示密码不匹配
   */
  public boolean mismatches(String rawPassword, String encodedPassword) {
    if (rawPassword == null || encodedPassword == null) {
      return true;
    }

    // 如果是BCrypt格式
    if (encodedPassword.startsWith(BCRYPT_PREFIX)) {
      String bcryptHash = encodedPassword.substring(BCRYPT_PREFIX.length());
      return !passwordEncoder.matches(rawPassword, bcryptHash);
    }

    // 如果是MD5格式（带前缀）
    if (encodedPassword.startsWith(MD5_PREFIX)) {
      String md5Hash = encodedPassword.substring(MD5_PREFIX.length());
      return verifyMd5Password(rawPassword, md5Hash, "检测到MD5密码，建议升级为BCrypt");
    }

    // 兼容旧数据：没有前缀的密码，假设是MD5格式
    // 检查长度：MD5是32位十六进制字符串，BCrypt通常是60位
    if (encodedPassword.length() == 32) {
      return verifyMd5Password(rawPassword, encodedPassword,
          "检测到旧格式MD5密码，建议升级为BCrypt");
    }

    // 尝试BCrypt验证（不带前缀的情况）
    try {
      return !passwordEncoder.matches(rawPassword, encodedPassword);
    } catch (Exception e) {
      log.warn("密码验证失败，格式可能不正确", e);
      return true;
    }
  }

  /**
   * 检查密码是否为MD5格式.
   *
   * @param encodedPassword 加密后的密码
   * @return 是否为MD5格式
   */
  public boolean isMd5(String encodedPassword) {
    if (encodedPassword == null) {
      return false;
    }
    return encodedPassword.startsWith(MD5_PREFIX)
        || (encodedPassword.length() == 32 && !encodedPassword.startsWith(BCRYPT_PREFIX));
  }

  /**
   * 验证MD5密码.
   *
   * @param rawPassword 原始密码
   * @param md5Hash     MD5哈希值
   * @param logMessage  匹配成功时的日志消息
   * @return true 表示密码不匹配，false 表示匹配
   */
  private boolean verifyMd5Password(String rawPassword, String md5Hash, String logMessage) {
    String inputMd5 = DigestUtils.md5DigestAsHex(rawPassword.getBytes(StandardCharsets.UTF_8));
    boolean matches = inputMd5.equals(md5Hash);

    // 如果MD5验证成功，建议升级为BCrypt（可选，这里只记录日志）
    if (matches) {
      log.info(logMessage);
    }
    return !matches;
  }
}
