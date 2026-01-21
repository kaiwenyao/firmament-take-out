package dev.kaiwen.utils;

import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.DigestUtils;

/**
 * 密码工具类 支持BCrypt和MD5两种加密方式，用于逐步迁移.
 */
@Slf4j
public class PasswordUtil {

  private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  // MD5密码的前缀标识，用于区分新旧密码
  private static final String MD5_PREFIX = "{MD5}";
  private static final String BCRYPT_PREFIX = "{BCRYPT}";

  private PasswordUtil() {
    // 工具类，禁止实例化
  }

  /**
   * 加密密码（使用BCrypt）.
   *
   * @param rawPassword 原始密码
   * @return 加密后的密码（带BCRYPT前缀）
   */
  public static String encode(String rawPassword) {
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
  public static boolean mismatches(String rawPassword, String encodedPassword) {
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
      String inputMd5 = DigestUtils.md5DigestAsHex(rawPassword.getBytes(StandardCharsets.UTF_8));
      boolean matches = inputMd5.equals(md5Hash);

      // 如果MD5验证成功，建议升级为BCrypt（可选，这里只记录日志）
      if (matches) {
        log.info("检测到MD5密码，建议升级为BCrypt");
      }
      return !matches;
    }

    // 兼容旧数据：没有前缀的密码，假设是MD5格式
    // 检查长度：MD5是32位十六进制字符串，BCrypt通常是60位
    if (encodedPassword.length() == 32) {
      // 可能是MD5格式
      String inputMd5 = DigestUtils.md5DigestAsHex(rawPassword.getBytes(StandardCharsets.UTF_8));
      boolean matches = inputMd5.equals(encodedPassword);

      if (matches) {
        log.info("检测到旧格式MD5密码，建议升级为BCrypt");
      }
      return !matches;
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
  public static boolean isMd5(String encodedPassword) {
    if (encodedPassword == null) {
      return false;
    }
    return encodedPassword.startsWith(MD5_PREFIX)
        || (encodedPassword.length() == 32 && !encodedPassword.startsWith(BCRYPT_PREFIX));
  }
}
